package com.scse.curriculum.topicclo.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.scse.curriculum.topicclo.dto.TopicCloRequest;
import com.scse.curriculum.topicclo.dto.TopicCloResponse;
import com.scse.curriculum.topicclo.service.TopicCloService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/topic-clos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'DEPT_HEAD', 'INSTRUCTOR')")
public class TopicCloController {

    private final TopicCloService topicCloService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public TopicCloResponse create(
            @RequestBody TopicCloRequest request) {

        return topicCloService.create(request);
    }

    @GetMapping
    public List<TopicCloResponse> getAll() {

        return topicCloService.getAll();
    }

    @GetMapping("/topic/{topicId}")
    public List<TopicCloResponse> getByTopic(
            @PathVariable Integer topicId) {

        return topicCloService.getByTopic(topicId);
    }

    @GetMapping("/clo/{cloId}")
    public List<TopicCloResponse> getByClo(
            @PathVariable Integer cloId) {

        return topicCloService.getByClo(cloId);
    }

    @DeleteMapping("/{topicId}/{cloId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public void delete(
            @PathVariable Integer topicId,
            @PathVariable Integer cloId) {

        topicCloService.delete(topicId, cloId);
    }
}