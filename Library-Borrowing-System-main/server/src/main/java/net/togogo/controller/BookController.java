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
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.getAllBooks(pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByTitle")
    public Result<PageResponse<BookDTO>> searchByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.searchByTitle(title, pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByAuthor")
    public Result<PageResponse<BookDTO>> searchByAuthor(
            @RequestParam String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        PageResponse<BookDTO> books = bookService.searchByAuthor(author, pageable);
        return Result.success(books);
    }

    @GetMapping("/searchByCategory")
    public Result<PageResponse<BookDTO>> searchByCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
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

}