package com.scse.curriculum.book.dto;

import com.scse.curriculum.book.entity.BookType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookResponse {

    private Integer id;

    private String title;

    private String author;

    private String publisher;

    private Integer year;

    private String edition;

    private String isbn;

    private String url;

    private BookType bookType;
}