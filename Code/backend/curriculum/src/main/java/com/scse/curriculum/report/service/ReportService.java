package com.scse.curriculum.report.service;

import com.scse.curriculum.report.dto.PloCoverageReportData;

public interface ReportService {
    byte[] generateCloPloMatrixExcel(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);
    byte[] generateCloPloMatrixPdf(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);
    byte[] generateCloPloMatrixExcel(long programId, Integer cohortId, String semester, String search, String status);
    byte[] generateCloPloMatrixPdf(long programId, Integer cohortId, String semester, String search, String status);
    byte[] generateSyllabusListExcel(String academicYear, String semester, Integer programId, Integer cohortId, String status);
    byte[] generateSyllabusListPdf(String academicYear, String semester, Integer programId, Integer cohortId, String status);
    PloCoverageReportData getPloCoverageReport(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);
    byte[] generatePloCoverageExcel(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);
    byte[] generatePloCoveragePdf(long programId, Integer cohortId, String academicYear, String semester, Integer courseTypeId);
}
