package com.scse.curriculum.topic.dto;

import com.scse.curriculum.topic.entity.TopicType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicResponse {

    private Integer id;

    private Integer syllabusId;

    private Integer weekNumber;

    private Integer orderInWeek;

    private String name;

    private String nameVn;

    private Integer teachingHours;

    private Integer labHours;

    private Integer selfStudyHours;

    private TopicType topicType;

    private String teachingMethod;

    private String learningActivity;

    private String notes;
}