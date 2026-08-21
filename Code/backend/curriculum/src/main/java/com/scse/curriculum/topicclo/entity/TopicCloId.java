package com.scse.curriculum.topicclo.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TopicCloId implements Serializable {

    @Column(name = "topic_id")
    private Integer topicId;

    @Column(name = "clo_id")
    private Integer cloId;
}