package com.scse.curriculum.topicclo.dto;

import com.scse.curriculum.topicclo.entity.TeachingLevel;

import lombok.Data;

@Data
public class TopicCloRequest {

    private Integer topicId;

    private Integer cloId;

    private TeachingLevel teachingLevel;
}