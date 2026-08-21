package com.scse.curriculum.deadline.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.deadline.dto.DeadlineDispatchResponse;
import com.scse.curriculum.deadline.dto.DeadlineEscalationDispatchResponse;
import com.scse.curriculum.deadline.dto.DeadlineEscalationLogResponse;
import com.scse.curriculum.deadline.dto.DeadlineEscalationPreviewResponse;
import com.scse.curriculum.deadline.dto.DeadlinePreviewResponse;
import com.scse.curriculum.deadline.dto.DeadlineReminderLogResponse;
import com.scse.curriculum.deadline.dto.SyllabusDeadlineResponse;
import com.scse.curriculum.deadline.dto.UpsertSyllabusDeadlineRequest;
import com.scse.curriculum.deadline.service.DeadlineEscalationService;
import com.scse.curriculum.deadline.service.SyllabusDeadlineService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/syllabus-deadlines")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SyllabusDeadlineAdminController {

    private final SyllabusDeadlineService service;
    private final DeadlineEscalationService escalationService;

    @GetMapping
    public List<SyllabusDeadlineResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public SyllabusDeadlineResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    public SyllabusDeadlineResponse create(
            @Valid @RequestBody UpsertSyllabusDeadlineRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public SyllabusDeadlineResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpsertSyllabusDeadlineRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/active")
    public SyllabusDeadlineResponse setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return service.setActive(id, active);
    }

    @GetMapping("/{id}/preview")
    public DeadlinePreviewResponse preview(@PathVariable Long id) {
        return service.preview(id);
    }

    @PostMapping("/{id}/dispatch-now")
    public DeadlineDispatchResponse dispatchNow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        return service.dispatchNow(id, force);
    }

    @GetMapping("/{id}/logs")
    public List<DeadlineReminderLogResponse> getLogs(@PathVariable Long id) {
        return service.getReminderLogs(id);
    }

    @GetMapping("/{id}/escalation-preview")
    public DeadlineEscalationPreviewResponse escalationPreview(
            @PathVariable Long id) {
        return escalationService.preview(id);
    }

    @PostMapping("/{id}/escalate-now")
    public DeadlineEscalationDispatchResponse escalateNow(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        return escalationService.dispatchNow(id, force);
    }

    @GetMapping("/{id}/escalation-logs")
    public List<DeadlineEscalationLogResponse> escalationLogs(
            @PathVariable Long id) {
        return escalationService.getLogs(id);
    }
}
