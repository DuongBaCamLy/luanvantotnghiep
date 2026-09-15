package com.scse.curriculum.syllabus.maintenance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/syllabuses/maintenance")
@RequiredArgsConstructor
public class SyllabusMaintenanceController {

    private final SyllabusCohortResetService cohortResetService;

    @DeleteMapping("/cohort")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> resetCohortForReimport(
            @RequestParam Integer programId,
            @RequestParam Integer cohortId,
            @RequestParam String confirmCohort) {

        int deletedCount =
                cohortResetService.resetForReimport(
                        programId,
                        cohortId,
                        confirmCohort);

        return ResponseEntity.ok(
                Map.of("deletedCount", deletedCount));
    }
}