package com.scse.curriculum.syllabusbook.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SyllabusBookId
        implements Serializable {

    private Integer syllabusId;

    private Integer bookId;
}