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

    @Column(nullable = false)
    private String title;

    private String author;

    private String publisher;

    private Integer year;

    private String edition;

    private String isbn;

    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "book_type")
    private BookType bookType;
}