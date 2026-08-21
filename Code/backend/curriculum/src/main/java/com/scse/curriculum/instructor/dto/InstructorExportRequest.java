package com.scse.curriculum.instructor.dto;

import lombok.Data;

import java.util.List;

@Data
public class InstructorExportRequest {

    /**
     * IDs selected by the frontend after applying the visible filters.
     * Empty/null means export all instructors.
     */
    private List<Integer> instructorIds;

    /**
     * Human-readable filter summary used only in the workbook header.
     */
    private String search;
    private String department;
    private String role;
    private String degree;
    private String status;
}