package net.togogo.service.impl;

import lombok.RequiredArgsConstructor;
import net.togogo.common.BusinessException;
import net.togogo.common.ResultCode;
import net.togogo.dto.BookDTO;
import net.togogo.dto.CreateBookRequest;
import net.togogo.dto.PageResponse;
import net.togogo.entity.Book;
import net.togogo.entity.BookArchive;
import net.togogo.entity.BorrowRecord;
import net.togogo.mapper.BookMapper;
import net.togogo.repository.BookArchiveRepository;
import net.togogo.repository.BookRepository;
import net.togogo.repository.BorrowRecordRepository;
import net.togogo.service.BookService;
import net.togogo.util.BookExcelImporter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final BookArchiveRepository bookArchiveRepository;
    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    @Transactional
    @CacheEvict(value = {"books", "books:all", "books:search"}, allEntries = true)
    public BookDTO createBook(CreateBookRequest request) {
        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException(ResultCode.BOOK_ISBN_EXIST);
        }

        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .publisher(request.getPublisher())
                .publishDate(request.getPublishDate())
                .category(request.getCategory())
                .description(request.getDescription())
                .stock(request.getStock())
                .available(request.getStock())
                .build();

        Book saved = bookRepository.save(book);
        return BookMapper.toDTO(saved);
    }

    @Override
    @Cacheable(value = "books", key = "#id")
    public BookDTO getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));
        return BookMapper.toDTO(book);
    }

    @Override
    @Cacheable(value = "books:all", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BookDTO> getAllBooks(Pageable pageable) {
        Page<BookDTO> page = bookRepository.findAll(pageable).map(BookMapper::toDTO);
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "books:search", key = "'title:' + #title + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BookDTO> searchByTitle(String title, Pageable pageable) {
        Page<BookDTO> page = bookRepository.findByTitleContaining(title, pageable).map(BookMapper::toDTO);
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "books:search", key = "'author:' + #author + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BookDTO> searchByAuthor(String author, Pageable pageable) {
        Page<BookDTO> page = bookRepository.findByAuthorContaining(author, pageable).map(BookMapper::toDTO);
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "books:search", key = "'category:' + #category + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BookDTO> searchByCategory(String category, Pageable pageable) {
        Page<BookDTO> page = bookRepository.findByCategory(category, pageable).map(BookMapper::toDTO);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"books", "books:all", "books:search"}, allEntries = true)
    public BookDTO updateBook(Long id, CreateBookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPublisher(request.getPublisher());
        book.setPublishDate(request.getPublishDate());
        book.setCategory(request.getCategory());
        book.setDescription(request.getDescription());
        book.setStock(request.getStock());

        // 校验：可借数量不能大于库存数量
        if (book.getAvailable() != null && book.getStock() != null
                && book.getAvailable() > book.getStock()) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        Book updated = bookRepository.save(book);
        return BookMapper.toDTO(updated);
    }
    //添加恢复的方法
    @Override
    @Transactional
    @CacheEvict(value = {"books", "books:all", "books:search"}, allEntries = true)
    public BookDTO restoreBook(Long id) {
        BookArchive archive = bookArchiveRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        // 从归档表删除，以便再次删除时可以重新归档
        bookArchiveRepository.delete(archive);

        Book book = Book.builder()
                .title(archive.getTitle())
                .author(archive.getAuthor())
                .isbn(archive.getIsbn())
                .publisher(archive.getPublisher())
                .publishDate(archive.getPublishDate())
                .category(archive.getCategory())
                .description(archive.getDescription())
                .stock(archive.getStock())
                .available(archive.getAvailable())
                .build();
        Book restored = bookRepository.save(book);
        return BookMapper.toDTO(restored);
    }
    @Override
    public PageResponse<BookDTO> getDeletedBooks(Pageable pageable) {
        Page<BookDTO> page = bookArchiveRepository.findByStatusOrderByDeleteTimeDesc("DELETED", pageable)
                .map(archive -> BookMapper.toDTO(archive.toBook()));
        return PageResponse.from(page);
    }
    // 清除过期图书
    @Override
    @Transactional
    @CacheEvict(value = {"books", "books:all", "books:search"}, allEntries = true)
    public void cleanExpiredBooks(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        bookArchiveRepository.deleteByDeleteTimeBefore(threshold);
    }
    @Override
    @Transactional
    public int batchImportFromExcel(MultipartFile file) {
        List<CreateBookRequest> books = BookExcelImporter.parse(file);
        int count = 0;
        for (CreateBookRequest req : books) {
            if (req.getIsbn() != null && bookRepository.existsByIsbn(req.getIsbn())) {
                throw new BusinessException(ResultCode.BOOK_ISBN_EXIST);
            }
            bookRepository.save(convertToEntity(req));
            count++;
        }
        return count;
    }

    @Override
    @Async("asyncExecutor")
    public void batchImportAsync(byte[] fileBytes, String originalFilename, String taskId) {
        try {
            redisTemplate.opsForValue().set("import:progress:" + taskId, "解析中...", 10, java.util.concurrent.TimeUnit.MINUTES);

            List<CreateBookRequest> books;
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                books = BookExcelImporter.parse(is);
            }

            redisTemplate.opsForValue().set("import:progress:" + taskId,
                    "解析完成，共 " + books.size() + " 条，开始导入...", 10, java.util.concurrent.TimeUnit.MINUTES);

            Integer insertedCount = transactionTemplate.execute(status -> {
                // 清理可能残留的临时表（MySQL 临时表生命周期绑定连接而非事务，异常回滚不会自动清理）
                jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS t_book_temp");
                jdbcTemplate.execute("CREATE TEMPORARY TABLE t_book_temp LIKE t_book");
                jdbcTemplate.execute("ALTER TABLE t_book_temp DROP PRIMARY KEY, MODIFY id BIGINT NOT NULL");

                batchInsertToTemp(books);

                int count = jdbcTemplate.update(
                        "INSERT INTO t_book (title, author, isbn, publisher, publish_date, category, description, stock, available) " +
                        "SELECT t.title, t.author, t.isbn, t.publisher, t.publish_date, t.category, t.description, t.stock, t.available " +
                        "FROM t_book_temp t " +
                        "LEFT JOIN t_book b ON b.isbn = t.isbn " +
                        "WHERE b.isbn IS NULL");

                jdbcTemplate.execute("DROP TEMPORARY TABLE t_book_temp");
                return count;
            });

            String result = "成功导入 " + insertedCount + " 条";
            redisTemplate.opsForValue().set("import:result:" + taskId, result, 10, java.util.concurrent.TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("import:progress:" + taskId, result, 10, java.util.concurrent.TimeUnit.MINUTES);

        } catch (Exception e) {
            String errorMsg = "导入失败: " + e.getMessage();
            redisTemplate.opsForValue().set("import:result:" + taskId, errorMsg, 10, java.util.concurrent.TimeUnit.MINUTES);
            redisTemplate.opsForValue().set("import:progress:" + taskId, errorMsg, 10, java.util.concurrent.TimeUnit.MINUTES);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public String getImportProgress(String taskId) {
        String progress = redisTemplate.opsForValue().get("import:progress:" + taskId);
        if (progress == null) {
            String result = redisTemplate.opsForValue().get("import:result:" + taskId);
            return result != null ? result : "任务不存在或已过期";
        }
        return progress;
    }

    private void batchInsertToTemp(List<CreateBookRequest> books) {
        String sql = "INSERT INTO t_book_temp (title, author, isbn, publisher, publish_date, category, description, stock, available) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, books, 100, (ps, req) -> {
            ps.setString(1, req.getTitle());
            ps.setString(2, req.getAuthor());
            ps.setString(3, req.getIsbn());
            ps.setString(4, req.getPublisher());
            if (req.getPublishDate() != null) {
                ps.setObject(5, req.getPublishDate());
            } else {
                ps.setObject(5, LocalDateTime.now());
            }
            ps.setString(6, req.getCategory());
            ps.setString(7, req.getDescription());
            int stock = req.getStock() != null ? req.getStock() : 1;
            ps.setInt(8, stock);
            ps.setInt(9, stock);
        });
    }

    // 转换为实体类
    private Book convertToEntity(CreateBookRequest req) {
        return Book.builder()
                .title(req.getTitle())
                .author(req.getAuthor())
                .isbn(req.getIsbn())
                .publisher(req.getPublisher())
                .publishDate(req.getPublishDate())
                .category(req.getCategory())
                .description(req.getDescription())
                .stock(req.getStock() != null ? req.getStock() : 1)
                .available(req.getStock() != null ? req.getStock() : 1)
                .build();
    }

    @Override
    @Transactional
    // 清除缓存
    // 清除缓存已由 @CacheEvict 注解处理
    @CacheEvict(value = {"books", "books:all", "books:search"}, allEntries = true)
    public void deleteBook(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        Long borrowedCount = borrowRecordRepository.countByBookIdAndStatus(id, BorrowRecord.Borrowstatus.BORROWED);
        if (borrowedCount > 0) {
            throw new BusinessException(ResultCode.BOOK_BORROWED_CANNOT_DELETE);
        }
        //先保存到归档表
        BookArchive archive = BookArchive.fromBook(book);
        bookArchiveRepository.save(archive);
        

        bookRepository.delete(book);

    }
    
}