package com.scse.curriculum.book.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "book")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String author;

    @Column(length = 255)
    private String publisher;

    private Integer year;

    private String edition;

    private String isbn;

    @Column(length = 1000)
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_type", nullable = false)
    @Builder.Default
    private BookType bookType = BookType.REFERENCE;

    @PrePersist
    private void initializeBookType() {
        if (bookType == null) bookType = BookType.REFERENCE;
    }
}
