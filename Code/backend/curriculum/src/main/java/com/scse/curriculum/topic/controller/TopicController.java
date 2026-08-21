package com.scse.curriculum.topic.controller;

import com.scse.curriculum.topic.dto.TopicRequest;
import com.scse.curriculum.topic.dto.TopicResponse;
import com.scse.curriculum.topic.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public TopicResponse create(
            @Valid @RequestBody TopicRequest request) {

        return topicService.create(request);
    }

    @GetMapping("/{id}")
    public TopicResponse getById(
            @PathVariable Integer id) {

        return topicService.getById(id);
    }

    @GetMapping
    public List<TopicResponse> getAll() {

        return topicService.getAll();
    }

    @GetMapping("/syllabus/{syllabusId}")
    public List<TopicResponse> getBySyllabus(
            @PathVariable Integer syllabusId) {

        return topicService.getBySyllabus(syllabusId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public TopicResponse update(
            @PathVariable Integer id,
            @Valid @RequestBody TopicRequest request) {

        return topicService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id) {

        topicService.delete(id);

        return ResponseEntity.noContent().build();
    }
}