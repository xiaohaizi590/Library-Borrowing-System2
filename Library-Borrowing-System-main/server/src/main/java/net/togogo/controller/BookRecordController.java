package net.togogo.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.togogo.common.Result;
import net.togogo.dto.BorrowRecordDTO;
import net.togogo.dto.BorrowRequest;
import net.togogo.dto.PageResponse;
import net.togogo.service.BookRecordService;

@RestController
@RequestMapping("/api/book-records")
@RequiredArgsConstructor
public class BookRecordController {

    private final BookRecordService bookRecordService;

    @PostMapping("/borrow")
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowRecordDTO> borrowBook(@Valid @RequestBody BorrowRequest request) {
        BorrowRecordDTO record = bookRecordService.borrowBook(request);
        return Result.success("借阅成功", record);
    }

    @PostMapping("/return/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowRecordDTO> returnBook(@PathVariable Long recordId) {
        BorrowRecordDTO record = bookRecordService.returnBook(recordId);
        return Result.success("归还成功", record);
    }

    @PostMapping("/renew/{recordId}")
    @PreAuthorize("isAuthenticated()")
    public Result<BorrowRecordDTO> renewBook(@PathVariable Long recordId) {
        BorrowRecordDTO record = bookRecordService.renewBook(recordId);
        return Result.success("续借成功", record);
    }

    @GetMapping("/borrowRecords/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResponse<BorrowRecordDTO>> getBorrowRecordsByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "borrowTime"));
        PageResponse<BorrowRecordDTO> records = bookRecordService.getBorrowRecordsByUser(userId, pageable);
        return Result.success(records);
    }

    @GetMapping("/borrowRecords/book/{bookId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<BorrowRecordDTO>> getBorrowRecordsByBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "borrowTime"));
        PageResponse<BorrowRecordDTO> records = bookRecordService.getBorrowRecordsByBook(bookId, pageable);
        return Result.success(records);
    }

    @GetMapping("/borrowRecords/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<BorrowRecordDTO>> getAllBorrowRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "borrowTime"));
        PageResponse<BorrowRecordDTO> records = bookRecordService.getAllBorrowRecords(pageable);
        return Result.success(records);
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<BorrowRecordDTO>> getOverdueRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
                //分页参数保护，防止无效分页参数
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));//以后可以如果多的话，可以进行全局管理
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "borrowTime"));
        PageResponse<BorrowRecordDTO> records = bookRecordService.getOverdueRecords(pageable);
        return Result.success(records);
    }
}
