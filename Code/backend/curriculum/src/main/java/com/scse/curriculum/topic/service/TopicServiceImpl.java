package com.scse.curriculum.topic.service;

import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.syllabus.entity.Syllabus;
import com.scse.curriculum.syllabus.repository.SyllabusRepository;
import com.scse.curriculum.syllabus.service.SyllabusContentGuard;
import com.scse.curriculum.topic.dto.TopicRequest;
import com.scse.curriculum.topic.dto.TopicResponse;
import com.scse.curriculum.topic.entity.Topic;
import com.scse.curriculum.topic.repository.TopicRepository;
import com.scse.curriculum.topic.service.TopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final SyllabusRepository syllabusRepository;
    private final SyllabusContentGuard syllabusContentGuard;

    @Override
    public TopicResponse create(
            TopicRequest request) {

        Syllabus syllabus = syllabusRepository
                .findById(request.getSyllabusId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Syllabus not found with id: "
                                        + request.getSyllabusId()));

        syllabusContentGuard.assertMutable(syllabus.getId());

        Topic topic = Topic.builder()
                .syllabus(syllabus)
                .weekNumber(request.getWeekNumber())
                .orderInWeek(request.getOrderInWeek())
                .name(request.getName())
                .nameVn(request.getNameVn())
                .teachingHours(hoursOrDefault(request.getTeachingHours(), 3))
                .labHours(hoursOrDefault(request.getLabHours(), 0))
                .selfStudyHours(hoursOrDefault(request.getSelfStudyHours(), 6))
                .topicType(request.getTopicType())
                .teachingMethod(request.getTeachingMethod())
                .learningActivity(request.getLearningActivity())
                .assessments(request.getAssessments())
                .resources(request.getResources())
                .notes(request.getNotes())
                .build();

        return mapToResponse(
                topicRepository.save(topic));
    }

    @Override
    public TopicResponse getById(
            Integer id) {

        Topic topic = topicRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found with id: "
                                        + id));

        syllabusContentGuard.assertCanView(topic.getSyllabus().getId());
        return mapToResponse(topic);
    }

    @Override
    public List<TopicResponse> getAll() {

        return topicRepository.findAll()
                .stream()
                .filter(topic -> syllabusContentGuard.canView(
                        topic.getSyllabus().getId()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<TopicResponse> getBySyllabus(
            Integer syllabusId) {

        syllabusContentGuard.assertCanView(syllabusId);
        return topicRepository
                .findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
                        syllabusId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public TopicResponse update(
            Integer id,
            TopicRequest request) {

        Topic topic = topicRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found with id: "
                                        + id));

        syllabusContentGuard.assertMutable(topic.getSyllabus().getId());

        Syllabus syllabus = syllabusRepository
                .findById(request.getSyllabusId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Syllabus not found with id: "
                                        + request.getSyllabusId()));

        syllabusContentGuard.assertMutable(syllabus.getId());
        topic.setSyllabus(syllabus);
        topic.setWeekNumber(request.getWeekNumber());
        topic.setOrderInWeek(request.getOrderInWeek());
        topic.setName(request.getName());
        topic.setNameVn(request.getNameVn());
        topic.setTeachingHours(hoursOrDefault(request.getTeachingHours(), 3));
        topic.setLabHours(hoursOrDefault(request.getLabHours(), 0));
        topic.setSelfStudyHours(hoursOrDefault(request.getSelfStudyHours(), 6));
        topic.setTopicType(request.getTopicType());
        topic.setTeachingMethod(request.getTeachingMethod());
        topic.setLearningActivity(request.getLearningActivity());
        topic.setAssessments(request.getAssessments());
        topic.setResources(request.getResources());
        topic.setNotes(request.getNotes());

        return mapToResponse(
                topicRepository.save(topic));
    }

    @Override
    public void delete(
            Integer id) {

        Topic topic = topicRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Topic not found with id: "
                                        + id));

        syllabusContentGuard.assertMutable(topic.getSyllabus().getId());
        topicRepository.delete(topic);
    }

    private TopicResponse mapToResponse(
            Topic topic) {

        return TopicResponse.builder()
                .id(topic.getId())
                .syllabusId(topic.getSyllabus().getId())
                .weekNumber(topic.getWeekNumber())
                .orderInWeek(topic.getOrderInWeek())
                .name(topic.getName())
                .nameVn(topic.getNameVn())
                .teachingHours(topic.getTeachingHours())
                .labHours(topic.getLabHours())
                .selfStudyHours(topic.getSelfStudyHours())
                .topicType(topic.getTopicType())
                .teachingMethod(topic.getTeachingMethod())
                .learningActivity(topic.getLearningActivity())
                .assessments(topic.getAssessments())
                .resources(topic.getResources())
                .notes(topic.getNotes())
                .build();
    }

    private int hoursOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }
}
