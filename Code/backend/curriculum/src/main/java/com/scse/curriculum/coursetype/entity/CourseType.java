package com.scse.curriculum.coursetype.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,
            unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_vn",
            nullable = false)
    private String nameVn;
}