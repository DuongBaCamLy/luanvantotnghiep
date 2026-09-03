package com.scse.curriculum.syllabus.dto;


import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseContextResponse {


    private Integer id;


    private String code;


    private String name;

}