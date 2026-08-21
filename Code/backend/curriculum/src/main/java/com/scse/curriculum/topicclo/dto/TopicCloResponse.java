package com.scse.curriculum.topicclo.dto;

import com.scse.curriculum.topicclo.entity.TeachingLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicCloResponse {

    private Integer topicId;

    private String topicName;

    private Integer cloId;

    private String cloCode;

    private TeachingLevel teachingLevel;
}