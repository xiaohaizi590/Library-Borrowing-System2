package net.togogo.controller;

import net.togogo.common.Result;
import net.togogo.dto.BookDTO;

import net.togogo.dto.CreateBookRequest;
import net.togogo.dto.PageResponse;
import net.togogo.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BookDTO> createBook(@Valid @RequestBody CreateBookRequest request) {
        BookDTO bookDTO = bookService.createBook(request);
        return Result.success("创建成功", bookDTO);
    }

    @GetMapping("/getById/{id}")
    public Result<BookDTO> getBookById(@PathVariable Long id) {
        BookDTO bookDTO = bookService.getBookById(id);
        return Result.success(bookDTO);
    }

    @GetMapping("/getAll")
    public Result<PageResponse<BookDTO>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.getAllBooks(pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByTitle")
    public Result<PageResponse<BookDTO>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.searchByTitle(title, pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByAuthor")
    public Result<PageResponse<BookDTO>> searchByAuthor(
            @RequestParam String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.searchByAuthor(author, pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByCategory")
    public Result<PageResponse<BookDTO>> searchByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.searchByCategory(category, pageable);
        return Result.success(books);
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BookDTO> updateBook(@PathVariable Long id, @RequestBody CreateBookRequest request) {
        BookDTO bookDTO = bookService.updateBook(id, request);
        return Result.success("更新成功", bookDTO);
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return Result.success();
    }

    @GetMapping("/recycleBin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResponse<BookDTO>> getDeletedBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        page = Math.max(0, Math.min(page, 100));
        size = Math.max(1, Math.min(size, 100));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.getDeletedBooks(pageable);
        return Result.success(books);
    }

    @PutMapping("/restore/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BookDTO> restoreBook(@PathVariable Long id) {
        BookDTO bookDTO = bookService.restoreBook(id);
        return Result.success("恢复成功", bookDTO);
    }

    @DeleteMapping("/cleanExpired")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> cleanExpiredBooks(@RequestParam(defaultValue = "30") int retentionDays) {
        bookService.cleanExpiredBooks(retentionDays);
        return Result.success();
    }

    @PostMapping(value = "/batch-import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> batchImport(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            return Result.error(400, "仅支持 .xlsx / .xls 格式");
        }
        int count = bookService.batchImportFromExcel(file);
        return Result.success("成功导入" + count + "本图书", count);
    }

    @PostMapping(value = "/batch-import-async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> batchImportAsync(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !(filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            return Result.error(400, "仅支持 .xlsx / .xls 格式");
        }

        String taskId = UUID.randomUUID().toString();
        byte[] fileBytes = file.getBytes();

        bookService.batchImportAsync(fileBytes, filename, taskId);

        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("message", "导入任务已提交，请通过任务ID查询进度");
        return Result.success(result);
    }

    @GetMapping("/batch-import/progress/{taskId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, String>> getImportProgress(@PathVariable String taskId) {
        String progress = bookService.getImportProgress(taskId);
        Map<String, String> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", progress);
        return Result.success(result);
    }

}