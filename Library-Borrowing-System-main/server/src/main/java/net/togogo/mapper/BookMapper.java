package net.togogo.mapper;

import net.togogo.dto.BookDTO;
import net.togogo.entity.Book;

public class BookMapper {

    public static BookDTO toDTO(Book book) {
        if (book == null) return null;
        return BookDTO.builder()
                .id(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .isbn(book.getIsbn())
                .publisher(book.getPublisher())
                .publishDate(book.getPublishDate())
                .category(book.getCategory())
                .description(book.getDescription())
                .stock(book.getStock())
                .available(book.getAvailable())
                .build();
    }
}
