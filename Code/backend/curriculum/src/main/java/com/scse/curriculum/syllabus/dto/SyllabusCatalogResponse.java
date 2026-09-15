package com.scse.curriculum.syllabus.dto;


import lombok.*;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyllabusCatalogResponse {


    private List<SyllabusCatalogItem> syllabuses;



    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SyllabusCatalogItem {


        private Integer id;


        /*
         * Course
         */
        private String courseCode;

        private String courseName;



        /*
         * Version
         */
        private String version; // Compatibility alias of versionLabel.
    private Integer versionNumber;
    private String versionLabel;



        /*
         * Program
         */
        private String program;



        /*
         * Created / Imported By
         */
        private String createdImportedBy;



        /*
         * Status
         */
        private String status;



        /*
         * Final Approval Date
         */
        private LocalDateTime finalApprovalDate;



        /*
         * Actions frontend dùng
         */
        private List<String> actions;

    }

}