package com.scse.curriculum.topic.repository;

import com.scse.curriculum.topic.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository
        extends JpaRepository<Topic, Integer> {

    List<Topic> findBySyllabusId(
            Integer syllabusId);

    List<Topic> findBySyllabusIdOrderByWeekNumberAscOrderInWeekAsc(
            Integer syllabusId);

    List<Topic> findByWeekNumber(
            Integer weekNumber);
}