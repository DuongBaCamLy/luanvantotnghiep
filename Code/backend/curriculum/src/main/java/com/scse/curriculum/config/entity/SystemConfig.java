package com.scse.curriculum.config.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    @Id
    @Column(name = "config_key", nullable = false)
    private String key;

    @Column(name = "config_value", nullable = false)
    private String value;

    private String description;
}
