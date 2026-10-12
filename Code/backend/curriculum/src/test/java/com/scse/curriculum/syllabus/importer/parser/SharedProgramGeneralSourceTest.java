package com.scse.curriculum.syllabus.importer.parser;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.*;
class SharedProgramGeneralSourceTest {
    @Test void inspectRuledGeneralCellsInActualEnglishSource()throws Exception {
        Path path=Path.of("../../../Tài Liệu/IT Program Specification IT - Full1.pdf");assumeTrue(Files.isRegularFile(path));
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(Files.readAllBytes(path))) {
            var cells=Cohort2027PdfTableRecovery.cells(pdf.getPage(173));
            var data=new com.scse.curriculum.syllabus.importer.dto.SyllabusImportData();
            Cohort2027PdfTableRecovery.recoverGeneral(cells,data);
            assertEquals("Lecture, lesson",data.getTeachingMethods());
            assertTrue(data.getPrerequisites().contains("complete IE2 course"));
            assertFalse(data.getPrerequisites().contains("When calculating"));
            assertFalse(data.getPrerequisites().contains("CLO1"));
        }
    }
}
