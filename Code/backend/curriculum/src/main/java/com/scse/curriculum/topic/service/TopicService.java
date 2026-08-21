package com.scse.curriculum.topic.service;

import com.scse.curriculum.topic.dto.TopicRequest;
import com.scse.curriculum.topic.dto.TopicResponse;

import java.util.List;

public interface TopicService {

    TopicResponse create(
            TopicRequest request);

    TopicResponse getById(
            Integer id);

    List<TopicResponse> getAll();

    List<TopicResponse> getBySyllabus(
            Integer syllabusId);

    TopicResponse update(
            Integer id,
            TopicRequest request);

    void delete(
            Integer id);
}