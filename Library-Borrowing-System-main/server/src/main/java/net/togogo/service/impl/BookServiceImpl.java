// server/src/main/java/net/togogo/service/impl/BookServiceImpl.java
package net.togogo.service.impl;

import lombok.RequiredArgsConstructor;
import net.togogo.common.BusinessException;
import net.togogo.common.ResultCode;
import net.togogo.dto.BookDTO;

import net.togogo.dto.CreateBookRequest;
import net.togogo.dto.PageResponse;
import net.togogo.entity.Book;
import net.togogo.entity.BorrowRecord;

import net.togogo.mapper.BookMapper;

import net.togogo.repository.BookRepository;
import net.togogo.repository.BorrowRecordRepository;
import net.togogo.service.BookService;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    

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

        bookRepository.delete(book);

    }

}