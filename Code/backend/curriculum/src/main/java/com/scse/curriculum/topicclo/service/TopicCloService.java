package com.scse.curriculum.topicclo.service;

import java.util.List;

import com.scse.curriculum.topicclo.dto.TopicCloRequest;
import com.scse.curriculum.topicclo.dto.TopicCloResponse;

public interface TopicCloService {

    TopicCloResponse create(
            TopicCloRequest request);

    List<TopicCloResponse> getAll();

    List<TopicCloResponse> getByTopic(
            Integer topicId);

    List<TopicCloResponse> getByClo(
            Integer cloId);

    void delete(
            Integer topicId,
            Integer cloId);
}