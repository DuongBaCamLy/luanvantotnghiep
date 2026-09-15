package com.scse.curriculum.syllabus.history;

import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.service.SyllabusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;

@RestController @RequestMapping("/api/syllabuses") @RequiredArgsConstructor
public class SyllabusHistoryController {
    private final SyllabusHistoryService history;
    private final SyllabusService syllabuses;

    @GetMapping("/{id}/revision-history")
    public List<SyllabusHistoryService.HistoryItem> history(@PathVariable Integer id) { return history.history(id); }

    @PostMapping("/{id}/revision")
    @PreAuthorize("hasAnyRole('ADMIN','INSTRUCTOR')")
    public SyllabusResponse revise(@PathVariable Integer id) { return syllabuses.createRevisionDraftFromRejected(id); }
}
