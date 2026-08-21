package com.scse.curriculum.topicclo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.clo.repository.CloRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topicclo.dto.TopicCloRequest;
import com.scse.curriculum.topicclo.dto.TopicCloResponse;
import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import com.scse.curriculum.topicclo.repository.TopicCloRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicCloServiceImpl implements TopicCloService {

    private final TopicCloRepository topicCloRepository;
    private final TopicRepository topicRepository;
    private final CloRepository cloRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public List<TopicCloResponse> getAll() {

        return topicCloRepository.findAll()
                .stream()
                .filter(mapping -> syllabusContentGuard.canView(
                        mapping.getTopic().getSyllabus().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TopicCloResponse> getByTopic(
            Integer topicId) {

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Topic not found"));
        syllabusContentGuard.assertCanView(topic.getSyllabus().getId());

        return topicCloRepository.findByIdTopicId(topicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TopicCloResponse> getByClo(
            Integer cloId) {

        Clo clo = cloRepository.findById(cloId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CLO not found"));
        syllabusContentGuard.assertCanView(clo.getSyllabus().getId());

        return topicCloRepository.findByIdCloId(cloId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TopicCloResponse create(
            TopicCloRequest request) {

        Topic topic = topicRepository.findById(
                request.getTopicId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found with id: "
                                        + request.getTopicId()));

        Clo clo = cloRepository.findById(
                request.getCloId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "CLO not found with id: "
                                        + request.getCloId()));

        Integer topicSyllabusId = topic.getSyllabus().getId();
        Integer cloSyllabusId = clo.getSyllabus().getId();
        syllabusContentGuard.assertMutable(topicSyllabusId);
        syllabusContentGuard.assertSameSyllabus(
                topicSyllabusId,
                cloSyllabusId,
                "Topic-CLO mapping");

        TopicCloId id = new TopicCloId(
                request.getTopicId(),
                request.getCloId());

        if (topicCloRepository.existsById(id)) {
            throw new RuntimeException(
                    "Topic-CLO mapping already exists");
        }

        TopicClo topicClo = TopicClo.builder()
                .id(id)
                .topic(topic)
                .clo(clo)
                .teachingLevel(
                        request.getTeachingLevel())
                .build();

        return toResponse(
                topicCloRepository.save(topicClo));
    }

    @Override
    public void delete(
            Integer topicId,
            Integer cloId) {

        TopicCloId id = new TopicCloId(
                topicId,
                cloId);

        TopicClo topicClo = topicCloRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic-CLO mapping not found"));

        syllabusContentGuard.assertMutable(
                topicClo.getTopic().getSyllabus().getId());
        topicCloRepository.delete(topicClo);
    }

    private TopicCloResponse toResponse(
            TopicClo topicClo) {

        return TopicCloResponse.builder()
                .topicId(topicClo.getTopic().getId())
                .topicName(topicClo.getTopic().getName())
                .cloId(topicClo.getClo().getId())
                .cloCode(topicClo.getClo().getCode())
                .teachingLevel(topicClo.getTeachingLevel())
                .build();
    }
}