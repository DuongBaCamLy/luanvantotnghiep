package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.testsupport.Cs2026DatabaseFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional(readOnly = true)
class SyllabusPdfCs2026DataAuditTest extends Cs2026DatabaseFixture {

    @Autowired
    private SyllabusPdfParser parser;


    @Autowired
    private CourseProgramRepository courseProgramRepository;

    @Autowired
    private CloPloMappingRepository cloPloMappingRepository;

    @Test
    void auditRealCs2026PdfAgainstCurrentDatabase() throws Exception {

        SourceDocument source =
                sourceDocumentRepository.findById(48L)
                        .orElseThrow(() ->
                                new AssertionError(
                                        "Source document 48 not found"
                                )
                        );

        SyllabusPdfParser.ParsedPdfBatch batch =
                parser.parsePdfBatchWithMetadata(
                        new ByteArrayInputStream(
                                source.getContent()
                        )
                );

        assertEquals(
                517,
                batch.pageCount()
        );

        assertEquals(
                55,
                batch.sections().size(),
                "Expected 55 syllabus sections"
        );

        Map<String, SyllabusPdfParser.ParsedSyllabusSection> parsedByCode =
                new LinkedHashMap<>();

        for (SyllabusPdfParser.ParsedSyllabusSection section
                : batch.sections()) {

            if (section.data() == null) {
                continue;
            }

            String code =
                    normalizeCode(
                            section.data()
                                    .getSourceCourseCode()
                    );

            if (!code.isBlank()) {
                parsedByCode.put(
                        code,
                        section
                );
            }
        }

        int cloDiffCourses = 0;
        int mappingDiffCourses = 0;
        int missingParsedCourses = 0;

        System.out.println();
        System.out.println(
                "========== CS2026 DATA AUDIT =========="
        );

        for (CourseProgram cp :
                courseProgramRepository
                        .findByProgramIdAndCohortIdWithRelations(
                                1,
                                12
                        )) {

            if (cp.getCourse() == null) {
                continue;
            }

            String courseCode =
                    cp.getCourse()
                            .getCourseCode();

            SyllabusPdfParser.ParsedSyllabusSection parsed =
                    parsedByCode.get(
                            normalizeCode(
                                    courseCode
                            )
                    );

            if (parsed == null) {

                missingParsedCourses++;

                System.out.println(
                        "NO_PARSED_SECTION | "
                                + courseCode
                );

                continue;
            }

            int parsedClos =
                    parsed.data()
                            .getClos()
                            .size();

            int parsedMappings =
                    parsed.data()
                            .getCloPloMappings()
                            .size();

            int dbClos =
                    cp.getSyllabus() == null
                            ? 0
                            : cp.getSyllabus()
                                    .getClos()
                                    .size();

            int dbMappings =
                    cp.getSyllabus() == null
                            ? 0
                            : cloPloMappingRepository
                                    .findByClo_Syllabus_Id(
                                            cp.getSyllabus()
                                                    .getId()
                                    )
                                    .size();

            boolean sameDepartment =
                    cp.getCourse()
                            .getDepartment() != null

                            && cp.getProgram() != null

                            && cp.getProgram()
                                    .getDepartment() != null

                            && cp.getCourse()
                                    .getDepartment()
                                    .getId()
                                    .equals(
                                            cp.getProgram()
                                                    .getDepartment()
                                                    .getId()
                                    );

            if (parsedClos != dbClos) {

                cloDiffCourses++;

                System.out.println(
                        "CLO_DIFF | "
                                + courseCode
                                + " | syllabusId="
                                + (
                                cp.getSyllabus() == null
                                        ? "NULL"
                                        : cp.getSyllabus()
                                                .getId()
                        )
                                + " | PDF="
                                + parsedClos
                                + " | DB="
                                + dbClos
                );
            }

            /*
             * Only compare syllabus-local outcome mappings
             * when the Course belongs to the same Department
             * as the target Program.
             *
             * MATH / PHYS / LANG / PE are intentionally
             * excluded from automatic CLO -> CS-PLO repair.
             */
            if (sameDepartment
                    && parsedMappings != dbMappings) {

                mappingDiffCourses++;

                System.out.println(
                        "MAPPING_DIFF | "
                                + courseCode
                                + " | syllabusId="
                                + (
                                cp.getSyllabus() == null
                                        ? "NULL"
                                        : cp.getSyllabus()
                                                .getId()
                        )
                                + " | PDF="
                                + parsedMappings
                                + " | DB="
                                + dbMappings
                );
            }
        }

        System.out.println(
                "---------------------------------------"
        );

        System.out.println(
                "CLO diff courses     = "
                        + cloDiffCourses
        );

        System.out.println(
                "Mapping diff courses = "
                        + mappingDiffCourses
        );

        System.out.println(
                "Missing parsed       = "
                        + missingParsedCourses
        );

        System.out.println(
                "======================================="
        );
    }

    private String normalizeCode(
            String value) {

        if (value == null) {
            return "";
        }

        String first =
                value.split(
                        "[/;,|]"
                )[0];

        String normalized =
                first.toUpperCase(
                                Locale.ROOT
                        )
                        .replaceAll(
                                "[^A-Z0-9]",
                                ""
                        );

        if (normalized.endsWith("IU")) {
            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 2
                    );
        }

        return normalized;
    }
}