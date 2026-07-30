package net.togogo.service.impl;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.togogo.common.BusinessException;
import net.togogo.common.ResultCode;
import net.togogo.dto.BorrowRecordDTO;
import net.togogo.dto.BorrowRequest;
import net.togogo.dto.PageResponse;
import net.togogo.entity.Book;
import net.togogo.entity.BorrowRecord;
import net.togogo.entity.User;
import net.togogo.mapper.BorrowRecordMapper;
import net.togogo.repository.BookRepository;
import net.togogo.repository.BorrowRecordRepository;
import net.togogo.repository.UserRepository;
import net.togogo.service.BookRecordService;


@Service
@RequiredArgsConstructor
public class BookRecordServiceImpl implements BookRecordService {
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    @CacheEvict(value = {"borrowRecords", "books", "books:all", "books:search"}, allEntries = true)
    public BorrowRecordDTO borrowBook(BorrowRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ResultCode.USER_NOT_FOUND));
        Long userId = currentUser.getId();

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        if (book.getAvailable() <= 0) {
            throw new BusinessException(ResultCode.BOOK_NOT_AVAILABLE);
        }

        boolean alreadyBorrowed = borrowRecordRepository.existsByBookIdAndUserIdAndStatus(
                request.getBookId(), userId, BorrowRecord.Borrowstatus.BORROWED);
        if (alreadyBorrowed) {
            throw new BusinessException(ResultCode.BOOK_ALREADY_BORROWED);
        }

        book.setAvailable(book.getAvailable() - 1);
        bookRepository.save(book);

        int days = request.getBorrowDays() != null ? request.getBorrowDays() : 30;
        LocalDateTime borrowTime = LocalDateTime.now();
        LocalDateTime dueTime = borrowTime.plusDays(days);

        BorrowRecord record = BorrowRecord.builder()
                .bookId(request.getBookId())
                .userId(userId)
                .borrowTime(borrowTime)
                .dueTime(dueTime)
                .status(BorrowRecord.Borrowstatus.BORROWED)
                .build();

        BorrowRecord saved = borrowRecordRepository.save(record);
        return BorrowRecordMapper.toDTO(saved, book.getTitle(), book.getAuthor(), currentUser.getUsername());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"borrowRecords", "books", "books:all", "books:search"}, allEntries = true)
    public BorrowRecordDTO returnBook(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        if (record.getStatus() != BorrowRecord.Borrowstatus.BORROWED) {
            throw new BusinessException(ResultCode.RECORD_NOT_BORROWED);
        }

        Book book = bookRepository.findById(record.getBookId())
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        book.setAvailable(book.getAvailable() + 1);
        bookRepository.save(book);

        record.setReturnTime(LocalDateTime.now());
        record.setStatus(BorrowRecord.Borrowstatus.RETURNED);

        BorrowRecord saved = borrowRecordRepository.save(record);
        User borrower = userRepository.findById(saved.getUserId()).orElse(null);
        String borrowerName = borrower != null ? borrower.getUsername() : "未知";
        return BorrowRecordMapper.toDTO(saved, book.getTitle(), book.getAuthor(), borrowerName);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"borrowRecords", "books", "books:all", "books:search"}, allEntries = true)
    public BorrowRecordDTO renewBook(Long recordId) {
        BorrowRecord record = borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        if (record.getStatus() != BorrowRecord.Borrowstatus.BORROWED) {
            throw new BusinessException(ResultCode.RECORD_NOT_BORROWED);
        }

        if (record.getRenewCount() >= 2) {
            throw new BusinessException(ResultCode.MAX_RENEW_COUNT_EXCEEDED);
        }

        record.setDueTime(record.getDueTime().plusDays(30));
        record.setRenewCount(record.getRenewCount() + 1);

        BorrowRecord saved = borrowRecordRepository.save(record);

        Book book = bookRepository.findById(record.getBookId()).orElse(null);
        String bookTitle = book != null ? book.getTitle() : "未知";
        String bookAuthor = book != null ? book.getAuthor() : "未知";
        User borrower = userRepository.findById(saved.getUserId()).orElse(null);
        String borrowerName = borrower != null ? borrower.getUsername() : "未知";
        return BorrowRecordMapper.toDTO(saved, bookTitle, bookAuthor, borrowerName);
    }

    /**
     * 批量查询 book/user 并组装 DTO，消除 N+1 问题
     */
    private List<BorrowRecordDTO> assembleWithBatchQuery(List<BorrowRecord> records) {
        // 收集所有 bookId 和 userId
        List<Long> bookIds = records.stream()
                .map(BorrowRecord::getBookId)
                .distinct()
                .collect(Collectors.toList());
        List<Long> userIds = records.stream()
                .map(BorrowRecord::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 两次批量查询取代 N*2 次单条查询
        Map<Long, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, b -> b));
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 内存组装
        return records.stream().map(record -> {
            Book book = bookMap.get(record.getBookId());
            String title = book != null ? book.getTitle() : "未知";
            String author = book != null ? book.getAuthor() : "未知";
            User user = userMap.get(record.getUserId());
            String username = user != null ? user.getUsername() : "未知";
            return BorrowRecordMapper.toDTO(record, title, author, username);
        }).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'user:' + #userId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getBorrowRecordsByUser(Long userId, Pageable pageable) {
        // 先分页查出借阅记录，再批量查询关联的 book 和 user（消除 N+1）
        Page<BorrowRecord> page = borrowRecordRepository.findByUserId(userId, pageable);
        List<BorrowRecordDTO> dtoList = assembleWithBatchQuery(page.getContent());
        Page<BorrowRecordDTO> dtoPage = new PageImpl<>(dtoList, pageable, page.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'book:' + #bookId")
    public PageResponse<BorrowRecordDTO> getBorrowRecordsByBook(Long bookId, Pageable pageable) {
        //分页
        Page<BorrowRecord> page = borrowRecordRepository.findByBookId(bookId, pageable);
        List<BorrowRecordDTO> dtoList = assembleWithBatchQuery(page.getContent());
        Page<BorrowRecordDTO> dtoPage = new PageImpl<>(dtoList, pageable, page.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'overdue:' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getOverdueRecords(Pageable pageable) {
        // 查出逾期记录后，批量查询 book 和 user（消除 N+1）
        Page<BorrowRecord> page = borrowRecordRepository.findByStatusAndDueTimeBefore(
                BorrowRecord.Borrowstatus.BORROWED, LocalDateTime.now(), pageable);
        List<BorrowRecordDTO> dtoList = assembleWithBatchQuery(page.getContent());
        Page<BorrowRecordDTO> dtoPage = new PageImpl<>(dtoList, pageable, page.getTotalElements());
        return PageResponse.from(dtoPage);
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'all:' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getAllBorrowRecords(Pageable pageable) {
        // 查出全部借阅记录后，批量查询 book 和 user（消除 N+1）
        Page<BorrowRecord> page = borrowRecordRepository.findAll(pageable);
        List<BorrowRecordDTO> dtoList = assembleWithBatchQuery(page.getContent());
        Page<BorrowRecordDTO> dtoPage = new PageImpl<>(dtoList, pageable, page.getTotalElements());
        return PageResponse.from(dtoPage);
    }

}
