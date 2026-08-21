package com.scse.curriculum.major.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "major")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false,
            unique = true,
            length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_vn",
            nullable = false)
    private String nameVn;
}