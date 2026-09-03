package com.scse.curriculum.syllabus.dto;


import lombok.*;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusFilterResponse {


    /*
     * Course ID / Course Name
     */
    private List<String> courses;



    /*
     * Canonical curriculum cohorts/program context (for example CS2024).
     */
    private List<String> programs;



    /*
     * Semester 1, Semester 2...
     */
    private List<String> semesters;



    /*
     * Draft, Approved...
     */
    private List<String> statuses;



    /*
     * Users
     */
    private List<String> users;

}
