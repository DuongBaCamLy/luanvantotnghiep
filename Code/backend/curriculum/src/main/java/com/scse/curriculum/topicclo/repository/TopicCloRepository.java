package com.scse.curriculum.topicclo.repository;

import com.scse.curriculum.topicclo.entity.TopicClo;
import com.scse.curriculum.topicclo.entity.TopicCloId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TopicCloRepository
        extends JpaRepository<TopicClo, TopicCloId> {

    List<TopicClo> findByIdTopicId(Integer topicId);

    List<TopicClo> findByIdCloId(Integer cloId);

    List<TopicClo> findByTopic_Syllabus_Id(Integer syllabusId);

    @Query("""
            SELECT topicClo
            FROM TopicClo topicClo
            JOIN FETCH topicClo.topic topic
            JOIN FETCH topicClo.clo clo
            WHERE topic.syllabus.id = :syllabusId
            ORDER BY topic.weekNumber, topic.orderInWeek, clo.orderIndex, clo.code
            """)
    List<TopicClo> findForPdfBySyllabusId(
            @Param("syllabusId") Integer syllabusId);
}
