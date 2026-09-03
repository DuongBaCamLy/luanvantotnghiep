package com.scse.curriculum.syllabus.dto;


import lombok.*;


import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusPrefillResponse {


    /*
     * Syllabus nguồn được dùng để prefill
     */
    private SyllabusResponse sourceSyllabus;



    /*
     * CLO list
     */
    private List<?> clos;



    /*
     * Teaching Content
     */
    private List<?> topics;



    /*
     * Assessment Plan
     */
    private List<?> assessments;



    /*
     * Reading List
     */
    private List<?> readingss;

}