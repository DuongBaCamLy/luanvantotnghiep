package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.testsupport.Cs2026DatabaseFixture;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SyllabusPdfCs2026WorkloadDiagnosticTest
        extends Cs2026DatabaseFixture {

    @Autowired
    private SyllabusPdfParser parser;

    @Test
    void printRawWorkloadBlocksForSuspiciousCs2026Courses()
            throws Exception {

        SourceDocument source =
                sourceDocumentRepository.findById(48L)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "CS2026 source document is missing"));

        byte[] pdfBytes =
                source.getContent();

        assertNotNull(pdfBytes);

        SyllabusPdfParser.ParsedPdfBatch batch =
                parser.parsePdfBatchWithMetadata(
                        new ByteArrayInputStream(pdfBytes));

        List<String> targets =
                List.of(
                        "IT013IU",
                        "IT058IU",
                        "IT079IU",
                        "IT090IU",
                        "IT091IU",
                        "IT120IU",
                        "IT153IU",
                        "IT177IU",
                        "PH016IU"
                );

        try (PDDocument document =
                     Loader.loadPDF(pdfBytes)) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            stripper.setSortByPosition(true);

            for (String target : targets) {

                SyllabusPdfParser.ParsedSyllabusSection section =
                        batch.sections()
                                .stream()
                                .filter(item ->
                                        item.data() != null
                                                && normalizeCode(target)
                                                .equals(
                                                        normalizeCode(
                                                                item.data()
                                                                        .getSourceCourseCode())))
                                .findFirst()
                                .orElseThrow(() ->
                                        new AssertionError(
                                                "Missing section for "
                                                        + target));

                stripper.setStartPage(
                        section.startPage());

                stripper.setEndPage(
                        section.endPage());

                String rawText =
                        stripper.getText(document);

                System.out.println();
                System.out.println(
                        "==================================================");
                System.out.println(
                        "COURSE = " + target);
                System.out.println(
                        "PAGES  = "
                                + section.startPage()
                                + "-"
                                + section.endPage());

                System.out.println(
                        "PARSED workloadTotal   = ["
                                + section.data()
                                .getWorkloadTotal()
                                + "]");

                System.out.println(
                        "PARSED workloadContact = ["
                                + section.data()
                                .getWorkloadContact()
                                + "]");

                System.out.println(
                        "PARSED workloadPrivate = ["
                                + section.data()
                                .getWorkloadPrivate()
                                + "]");

                System.out.println(
                        "RAW WORKLOAD BLOCK:");

                System.out.println(
                        workloadBlock(rawText));
            }
        }
    }

    private static String workloadBlock(
            String rawText) {

        if (rawText == null
                || rawText.isBlank()) {
            return "<EMPTY>";
        }

        String normalized =
                rawText.replace('\u00a0', ' ')
                        .replace("\r\n", "\n")
                        .replace('\r', '\n');

        int start =
                indexOfIgnoreCase(
                        normalized,
                        "Workload",
                        0);

        if (start < 0) {
            return "<WORKLOAD NOT FOUND>";
        }

        int end =
                indexOfIgnoreCase(
                        normalized,
                        "Credit points",
                        start);

        if (end < 0) {
            end =
                    Math.min(
                            normalized.length(),
                            start + 1800);
        }

        return normalized
                .substring(
                        start,
                        end)
                .trim();
    }

    private static int indexOfIgnoreCase(
            String source,
            String target,
            int fromIndex) {

        return source.toLowerCase(Locale.ROOT)
                .indexOf(
                        target.toLowerCase(Locale.ROOT),
                        fromIndex);
    }

    private static String normalizeCode(
            String value) {

        if (value == null) {
            return "";
        }

        String normalized =
                value.toUpperCase(Locale.ROOT)
                        .replaceAll(
                                "[^A-Z0-9]",
                                "");

        if (normalized.endsWith("IU")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 2);
        }

        return normalized;
    }
}