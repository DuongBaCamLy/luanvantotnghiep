package com.scse.curriculum.syllabus.comparison.dto;

import com.scse.curriculum.syllabus.comparison.model.SemanticAnalysisStatus;
import com.scse.curriculum.syllabus.comparison.model.SemanticSignificance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSyllabusDiffResponse {

    private Integer oldSyllabusId;

    private Integer newSyllabusId;

    private String oldVersionLabel;

    private String newVersionLabel;

    private Integer courseId;

    private String courseCode;

    private String courseName;

    private SemanticAnalysisStatus status;

    /**
     * True if at least one confirmed semantic result
     * represents a meaningful academic change.
     */
    private boolean hasMeaningfulChanges;

    /**
     * True when at least one item still requires AI and
     * could not be resolved.
     */
    private boolean hasUnresolvedItems;

    private SemanticSignificance overallSignificance;

    private String summary;

    @Builder.Default
    private List<SemanticDiffResult> items =
            new ArrayList<>();
}