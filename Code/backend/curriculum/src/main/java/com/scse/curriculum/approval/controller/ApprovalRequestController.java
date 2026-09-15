package com.scse.curriculum.approval.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.scse.curriculum.approval.dto.ApprovalHistoryResponse;
import com.scse.curriculum.approval.dto.ApprovalResponse;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStep;
import com.scse.curriculum.approval.service.ApprovalRequestService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/approval-requests")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService service;

    @PutMapping("/{id}/review")
@PreAuthorize("hasAnyRole('DEAN', 'DEPT_HEAD')")
    public ApprovalResponse review(
            @PathVariable Integer id,
            @Valid @RequestBody ReviewApprovalRequest request) {

        return service.review(id, request);
    }

    @GetMapping("/pending/{step}")
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD')")
    public List<ApprovalResponse> getPendingByStep(
            @PathVariable ApprovalStep step) {

        return service.getPendingByStep(step);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ApprovalResponse> getAll() {

        return service.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public ApprovalResponse getById(
            @PathVariable Integer id) {

        return service.getById(id);
    }

    @GetMapping("/syllabus/{syllabusId}")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<ApprovalResponse> getBySyllabus(
            @PathVariable Integer syllabusId) {

        return service.getBySyllabus(syllabusId);
    }

    /**
     * FR-05.8:
     * Trưởng bộ môn và Trưởng khoa xem toàn bộ lịch sử
     * review/comment của một syllabus.
     */
    @GetMapping("/syllabus/{syllabusId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
    public List<ApprovalHistoryResponse> getApprovalHistory(
            @PathVariable Integer syllabusId) {

        return service.getApprovalHistory(syllabusId);
    }
}