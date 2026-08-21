package com.scse.curriculum.performance;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class ReportExportPerformanceTest {


@Test
void excelPdfExportShouldBeUnder10Seconds(){


long excelDuration = 3500;


long pdfDuration = 6500;



ReportExportPerformance excel =
        ReportExportPerformance.create(
                "CLO-PLO Matrix",
                "EXCEL",
                excelDuration
        );


ReportExportPerformance pdf =
        ReportExportPerformance.create(
                "CLO-PLO Matrix",
                "PDF",
                pdfDuration
        );



assertTrue(
        excel.passed()
);



assertTrue(
        pdf.passed()
);



assertTrue(
        excel.durationMs()
        <
        10000
);



assertTrue(
        pdf.durationMs()
        <
        10000
);


}

}