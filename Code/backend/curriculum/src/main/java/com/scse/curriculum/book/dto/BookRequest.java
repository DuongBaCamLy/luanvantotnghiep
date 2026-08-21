package com.scse.curriculum.book.dto;

import com.scse.curriculum.book.entity.BookType;
import lombok.Data;

@Data
public class BookRequest {

    private String title;

    private String author;

    private String publisher;

    private Integer year;

    private String edition;

    private String isbn;

    private String url;

    private BookType bookType;
}