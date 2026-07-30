package net.togogo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Audited
@Table(name = "t_book_archive")
public class BookArchive {
    @Id
    private Long id;//保留id，可能会有逻辑错误

    @Column(nullable = false, length = 200)
    private String title;           // 书名

    @Column(nullable = false, length = 100)
    private String author;          // 作者

    @Column(length = 50,unique = true)
    private String isbn;            // ISBN编号

    @Column(length = 100)
    private String publisher;       // 出版社

    @Column(name = "publish_date")
    private LocalDateTime publishDate; // 出版日期

    @Column(length = 50)
    private String category;        // 分类（如：科技、文学、历史）

    @Column(columnDefinition = "TEXT")
    private String description;     // 简介

    @Column(nullable = false)
    private Integer stock;          // 库存数量

    @Column(nullable = false)
    private Integer available;      // 可借数量

    @Column(name = "create_time")
    private LocalDateTime createTime; // 创建时间

    @Column(name = "update_time")
    private LocalDateTime updateTime; // 更新时间

    @Column(name = "delete_time",nullable = false)//nullable = false 表示该字段不能为空，不能为 null
    private LocalDateTime deleteTime; // 删除时间

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "DELETED";

    // 从 Book 实体创建 BookArchive 实体
    public static BookArchive fromBook(Book book) {
        return BookArchive.builder()
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
                .createTime(book.getCreateTime())
                .updateTime(book.getUpdateTime())
                .deleteTime(LocalDateTime.now())
                .build();
    }
    public Book toBook(){
        return Book.builder()
                .id(id)
                .title(title)
                .author(author)
                .isbn(isbn)
                .publisher(publisher)
                .publishDate(publishDate)
                .category(category)
                .description(description)
                .stock(stock)
                .available(available)
                .createTime(createTime)
                .updateTime(updateTime)
                .build();
    }

    public void markAsRestored() {
        this.status = "RESTORED";
    }

    
}
