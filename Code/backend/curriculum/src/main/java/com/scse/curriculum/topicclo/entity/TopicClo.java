package com.scse.curriculum.topicclo.entity;

import com.scse.curriculum.clo.entity.Clo;
import com.scse.curriculum.topic.entity.Topic;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "topic_clo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicClo {

    @EmbeddedId
    private TopicCloId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id")
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cloId")
    @JoinColumn(name = "clo_id")
    private Clo clo;

    @Enumerated(EnumType.STRING)
    @Column(name = "teaching_level")
    private TeachingLevel teachingLevel;
}