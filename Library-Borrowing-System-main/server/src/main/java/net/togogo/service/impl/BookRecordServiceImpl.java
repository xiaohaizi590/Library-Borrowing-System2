package net.togogo.service.impl;

import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
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
        // 从 SecurityContext 中获取当前登录用户
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

    @Override
    @Cacheable(value = "borrowRecords", key = "'user:' + #userId + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getBorrowRecordsByUser(Long userId, Pageable pageable) {
        Page<BorrowRecordDTO> page = borrowRecordRepository.findByUserId(userId, pageable)
                .map(record -> {
                    Book b = bookRepository.findById(record.getBookId()).orElse(null);
                    String title = b != null ? b.getTitle() : "未知";
                    String author = b != null ? b.getAuthor() : "未知";
                    User u = userRepository.findById(record.getUserId()).orElse(null);
                    String uname = u != null ? u.getUsername() : "未知";
                    return BorrowRecordMapper.toDTO(record, title, author, uname);
                });
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'book:' + #bookId")
    public List<BorrowRecordDTO> getBorrowRecordsByBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND));

        return borrowRecordRepository.findByBookIdOrderByBorrowTimeDesc(bookId).stream()
                .map(record -> {
                    User user = userRepository.findById(record.getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "未知";
                    return BorrowRecordMapper.toDTO(record, book.getTitle(), book.getAuthor(), username);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'overdue:' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getOverdueRecords(Pageable pageable) {
        Page<BorrowRecordDTO> page = borrowRecordRepository.findByStatusAndDueTimeBefore(
                BorrowRecord.Borrowstatus.BORROWED, LocalDateTime.now(), pageable)
                .map(record -> {
                    Book book = bookRepository.findById(record.getBookId()).orElse(null);
                    String title = book != null ? book.getTitle() : "未知";
                    String author = book != null ? book.getAuthor() : "未知";
                    User user = userRepository.findById(record.getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "未知";
                    return BorrowRecordMapper.toDTO(record, title, author, username);
                });
        return PageResponse.from(page);
    }

    @Override
    @Cacheable(value = "borrowRecords", key = "'all:' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public PageResponse<BorrowRecordDTO> getAllBorrowRecords(Pageable pageable) {
        Page<BorrowRecordDTO> page = borrowRecordRepository.findAll(pageable)
                .map(record -> {
                    Book book = bookRepository.findById(record.getBookId()).orElse(null);
                    String title = book != null ? book.getTitle() : "未知";
                    String author = book != null ? book.getAuthor() : "未知";
                    User user = userRepository.findById(record.getUserId()).orElse(null);
                    String username = user != null ? user.getUsername() : "未知";
                    return BorrowRecordMapper.toDTO(record, title, author, username);
                });
        return PageResponse.from(page);
    }

}