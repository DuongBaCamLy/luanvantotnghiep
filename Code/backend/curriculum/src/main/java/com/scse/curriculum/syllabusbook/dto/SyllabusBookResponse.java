package com.scse.curriculum.syllabusbook.dto;

import com.scse.curriculum.syllabusbook.entity.UsageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusBookResponse {

    private Integer syllabusId;

    private Integer bookId;

    private String bookTitle;

    private String author;

    private String publisher;

    private Integer year;

    private String edition;

    private String isbn;

    private String url;

    private UsageType usageType;

    private Integer orderIndex;
}