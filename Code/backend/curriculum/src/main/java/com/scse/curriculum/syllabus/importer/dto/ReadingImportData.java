package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingImportData {



    private String title;



    private String author;



    private String edition;



    private String year;



    private String publisher;



    private String type;


}