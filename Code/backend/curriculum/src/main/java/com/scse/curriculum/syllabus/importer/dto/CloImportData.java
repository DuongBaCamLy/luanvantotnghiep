package com.scse.curriculum.syllabus.importer.dto;


import lombok.*;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloImportData {



    /*
     * Example:
     *
     * CLO1
     */
    private String code;



    /*
     * CLO description
     */
    private String description;



    /*
     * Vietnamese description
     */
    private String descriptionVn;



    /*
     * Bloom taxonomy
     *
     * Remember
     * Understand
     * Apply
     */
    private String bloomLevel;



    /*
     * Competency level
     */
    private String competencyLevel;



    private Integer orderIndex;


}