package com.scse.curriculum.programtype.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "program_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,
            unique = true,
            length = 50)
    private String code;

    @Column(nullable = false)
    private String name;
}