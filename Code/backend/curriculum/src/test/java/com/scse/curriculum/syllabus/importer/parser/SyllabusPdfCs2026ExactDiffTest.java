package com.scse.curriculum.syllabus.importer.parser;

import com.scse.curriculum.cloplomapping.entity.CloPloMapping;
import com.scse.curriculum.cloplomapping.entity.ContributionLevel;
import com.scse.curriculum.cloplomapping.repository.CloPloMappingRepository;
import com.scse.curriculum.courseprogram.entity.CourseProgram;
import com.scse.curriculum.courseprogram.repository.CourseProgramRepository;
import com.scse.curriculum.plo.repository.PloRepository;
import com.scse.curriculum.syllabus.source.entity.SourceDocument;
import com.scse.curriculum.testsupport.Cs2026DatabaseFixture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional(readOnly = true)
class SyllabusPdfCs2026ExactDiffTest extends Cs2026DatabaseFixture {

    @Autowired
    private SyllabusPdfParser parser;


    @Autowired
    private CourseProgramRepository courseProgramRepository;

    @Autowired
    private CloPloMappingRepository cloPloMappingRepository;

    @Autowired
    private PloRepository ploRepository;

    @Test
    void printExactCs2026CloAndMappingDiffs() throws Exception {

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
                batch.sections().size()
        );

        Map<String, SyllabusPdfParser.ParsedSyllabusSection>
                parsedByCode = new LinkedHashMap<>();

        for (SyllabusPdfParser.ParsedSyllabusSection section
                : batch.sections()) {

            if (section.data() == null) {
                continue;
            }

            String code =
                    normalizeCourseCode(
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

        Set<String> activePloCodes =
                new LinkedHashSet<>();

        ploRepository.findByProgramId(1)
                .stream()
                .filter(plo ->
                        plo.getIsActive() == null
                                || plo.getIsActive()
                )
                .forEach(plo ->
                        activePloCodes.add(
                                normalizeOutcomeCode(
                                        plo.getCode()
                                )
                        )
                );

        System.out.println();
        System.out.println(
                "========== CS2026 EXACT DIFF =========="
        );

        int coursesWithCloDiff = 0;
        int coursesWithMappingDiff = 0;

        for (CourseProgram cp :
                courseProgramRepository
                        .findByProgramIdAndCohortIdWithRelations(
                                1,
                                12
                        )) {

            if (cp.getCourse() == null
                    || cp.getSyllabus() == null) {
                continue;
            }

            String courseCode =
                    cp.getCourse()
                            .getCourseCode();

            SyllabusPdfParser.ParsedSyllabusSection parsed =
                    parsedByCode.get(
                            normalizeCourseCode(
                                    courseCode
                            )
                    );

            if (parsed == null) {
                continue;
            }

            /*
             * =================================================
             * CLO exact diff
             * =================================================
             */

            Set<String> pdfClos =
                    new LinkedHashSet<>();

            if (parsed.data().getClos() != null) {
                parsed.data()
        .getClos()
        .forEach(clo -> {

            String code =
                    normalizeCloCode(
                            clo.getCode()
                    );

            if (!isSourceExcludedClo(
                    courseCode,
                    code
            )) {
                pdfClos.add(code);
            }
        });
            }

            Set<String> dbClos =
                    new LinkedHashSet<>();

            cp.getSyllabus()
                    .getClos()
                    .forEach(clo ->
                            dbClos.add(
                                    normalizeCloCode(
                                            clo.getCode()
                                    )
                            )
                    );

            Set<String> missingClos =
                    difference(
                            pdfClos,
                            dbClos
                    );

            Set<String> extraClos =
                    difference(
                            dbClos,
                            pdfClos
                    );

            if (!missingClos.isEmpty()
                    || !extraClos.isEmpty()) {

                coursesWithCloDiff++;

                System.out.println();
                System.out.println(
                        "COURSE " + courseCode
                );

                System.out.println(
                        "  syllabusId = "
                                + cp.getSyllabus()
                                .getId()
                );

                System.out.println(
                        "  MISSING_CLO = "
                                + missingClos
                );
for (String missingCode :
        missingClos) {

    parsed.data()
            .getClos()
            .stream()
            .filter(clo ->
                    missingCode.equals(
                            normalizeCloCode(
                                    clo.getCode()
                            )
                    )
            )
            .findFirst()
            .ifPresent(clo ->
                    System.out.println(
                            "    "
                                    + missingCode
                                    + " => "
                                    + clo.getDescription()
                    )
            );
}
                System.out.println(
                        "  EXTRA_CLO   = "
                                + extraClos
                );
            }

            /*
             * Only syllabus-local matrices belonging to
             * the same department as the Program may
             * automatically become CS CLO->PLO mappings.
             */
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

            if (!sameDepartment) {
                continue;
            }

            /*
             * =================================================
             * PDF expected CLO->PLO mappings
             * =================================================
             */

            Map<String, ContributionLevel> pdfMappings =
                    new LinkedHashMap<>();

            if (parsed.data()
                    .getCloPloMappings() != null) {

                parsed.data()
                        .getCloPloMappings()
                        .forEach(mapping -> {

                            String clo =
                                    normalizeCloCode(
                                            mapping.getCloCode()
                                    );

                            String plo =
                                    normalizeOutcomeCode(
                                            mapping.getPloCode()
                                    );

                            /*
                             * Ignore stale/non-active PLO codes
                             * such as the old PLO7.
                             */
                            if (clo.isBlank()
        || isSourceExcludedClo(
                courseCode,
                clo
        )
        || !pdfClos.contains(clo)
        || !activePloCodes.contains(
                plo
        )) {
    return;
}

                            ContributionLevel level =
                                    expectedLevel(
                                            mapping.getValue()
                                    );

                            if (level == null) {
                                return;
                            }

                            pdfMappings.put(
                                    clo + "->" + plo,
                                    level
                            );
                        });
            }

            /*
             * =================================================
             * Current DB CLO->PLO mappings
             * =================================================
             */

            Map<String, ContributionLevel> dbMappings =
                    new LinkedHashMap<>();

            List<CloPloMapping> currentMappings =
                    cloPloMappingRepository
                            .findByClo_Syllabus_Id(
                                    cp.getSyllabus()
                                            .getId()
                            );

            for (CloPloMapping mapping
                    : currentMappings) {

                if (mapping.getClo() == null
                        || mapping.getPlo() == null) {
                    continue;
                }

                String plo =
                        normalizeOutcomeCode(
                                mapping.getPlo()
                                        .getCode()
                        );

                if (!activePloCodes.contains(plo)) {
                    continue;
                }

                String key =
                        normalizeCloCode(
                                mapping.getClo()
                                        .getCode()
                        )
                                + "->"
                                + plo;

                dbMappings.put(
                        key,
                        mapping.getLevel()
                );
            }

            Set<String> missingMappings =
                    difference(
                            pdfMappings.keySet(),
                            dbMappings.keySet()
                    );

            Set<String> extraMappings =
                    difference(
                            dbMappings.keySet(),
                            pdfMappings.keySet()
                    );

            Set<String> levelDiff =
                    new LinkedHashSet<>();

            for (String key :
                    pdfMappings.keySet()) {

                if (dbMappings.containsKey(key)
                        && pdfMappings.get(key)
                        != dbMappings.get(key)) {

                    levelDiff.add(
                            key
                                    + " PDF="
                                    + pdfMappings.get(key)
                                    + " DB="
                                    + dbMappings.get(key)
                    );
                }
            }

            if (!missingMappings.isEmpty()
                    || !extraMappings.isEmpty()
                    || !levelDiff.isEmpty()) {

                coursesWithMappingDiff++;

                System.out.println();
                System.out.println(
                        "MAPPING COURSE "
                                + courseCode
                );

                System.out.println(
                        "  syllabusId = "
                                + cp.getSyllabus()
                                .getId()
                );

                System.out.println(
                        "  MISSING_MAPPING = "
                                + missingMappings
                );
for (String key :
        missingMappings) {

    System.out.println(
            "    "
                    + key
                    + " level="
                    + pdfMappings.get(key)
    );
}
                System.out.println(
                        "  EXTRA_MAPPING   = "
                                + extraMappings
                );

                System.out.println(
                        "  LEVEL_DIFF      = "
                                + levelDiff
                );
            }
        }

        System.out.println();
        System.out.println(
                "---------------------------------------"
        );

        System.out.println(
                "Courses with CLO exact diff     = "
                        + coursesWithCloDiff
        );

        System.out.println(
                "Courses with mapping exact diff = "
                        + coursesWithMappingDiff
        );

        System.out.println(
                "======================================="
        );
    }

    private ContributionLevel expectedLevel(
            String value) {

        String normalized =
                value == null
                        ? ""
                        : value.trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        return switch (normalized) {
            case "X", "I", "1" ->
                    ContributionLevel.I;

            case "XX", "D", "2" ->
                    ContributionLevel.D;

            case "XXX", "A", "3" ->
                    ContributionLevel.A;

            default ->
                    null;
        };
    }

    private Set<String> difference(
            Set<String> left,
            Set<String> right) {

        Set<String> result =
                new LinkedHashSet<>(
                        left
                );

        result.removeAll(
                right
        );

        return result;
    }

    private String normalizeCloCode(
            String value) {

        if (value == null) {
            return "";
        }

        return value.toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^A-Z0-9]",
                        ""
                );
    }

    private String normalizeOutcomeCode(
            String value) {

        if (value == null) {
            return "";
        }

        return value.toUpperCase(
                        Locale.ROOT
                )
                .replaceAll(
                        "[^A-Z0-9]",
                        ""
                );
    }

    private String normalizeCourseCode(
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
    private boolean isSourceExcludedClo(
        String courseCode,
        String cloCode) {

    return "IT165IU".equalsIgnoreCase(
            courseCode
    )
            && "CLO6".equals(
                    normalizeCloCode(
                            cloCode
                    )
            );
}
}