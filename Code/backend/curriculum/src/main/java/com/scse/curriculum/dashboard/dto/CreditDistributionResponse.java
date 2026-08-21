package com.scse.curriculum.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreditDistributionResponse {
    private Integer programId;
    private String programCode;
    private String programName;
    private String programNameVn;
    private Integer cohortId;
    private String cohortName;
    private Integer cohortEntryYear;
    private Integer declaredProgramCredits;
    private Integer calculatedTotalCredits;
    private Integer creditDifference;
    private Boolean matchesDeclaredTotal;
    private Integer uniqueCourseCount;
    private Integer duplicateRowsRemoved;
    private String dataSource;
    private List<CreditGroup> groups;
    private List<DataWarning> warnings;

    @Data
    @Builder
    public static class CreditGroup {
        private Integer courseTypeId;
        private String code;
        private String name;
        private String nameVn;
        private Integer credits;
        private Integer courseCount;
        private Double percentage;
    }

    @Data
    @Builder
    public static class DataWarning {
        private String code;
        private String message;
    }
}
