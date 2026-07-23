// server/src/main/java/net/togogo/service/BookService.java
package net.togogo.service;

import net.togogo.dto.BookDTO;
import net.togogo.dto.CreateBookRequest;
import net.togogo.dto.PageResponse;
import org.springframework.data.domain.Pageable;


public interface BookService {
    BookDTO createBook(CreateBookRequest request);
    BookDTO getBookById(Long id);
    PageResponse<BookDTO> getAllBooks(Pageable pageable);
    PageResponse<BookDTO> searchByTitle(String title, Pageable pageable);
    PageResponse<BookDTO> searchByAuthor(String author, Pageable pageable);
    PageResponse<BookDTO> searchByCategory(String category, Pageable pageable);
    BookDTO updateBook(Long id, CreateBookRequest request);
    void deleteBook(Long id);

}