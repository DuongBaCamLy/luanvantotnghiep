package com.scse.curriculum.syllabus.dto;


import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyllabusListItemResponse {


    private Integer id;


    /*
     * Column:
     * ID Course
     */
    private String courseCode;



    /*
     * Column:
     * Course Name
     */
    private String courseName;



    /*
     * Column:
     * Version
     */
    private String version; // Compatibility alias of versionLabel.
    private Integer versionNumber;
    private String versionLabel;



    /*
     * Column:
     * Program
     */
    private String program;



    /*
     * Column:
     * Semester
     */
    private String semester;



    /*
     * Column:
     * Created / Imported By
     */
    private String createdImportedBy;



    /*
     * Column:
     * Status
     */
    private String status;



    /*
     * Column:
     * Final Approval Date
     */
    private LocalDateTime finalApprovalDate;

}