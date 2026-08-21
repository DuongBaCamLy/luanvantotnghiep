package com.scse.curriculum.config.controller;

import com.scse.curriculum.config.dto.SystemConfigRequest;
import com.scse.curriculum.config.entity.SystemConfig;
import com.scse.curriculum.config.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService service;

    @GetMapping
    public ResponseEntity<List<SystemConfig>> getAllConfigs() {
        return ResponseEntity.ok(service.getAllConfigs());
    }

    @GetMapping("/{key}")
    public ResponseEntity<SystemConfig> getConfig(@PathVariable String key) {
        SystemConfig config = service.getConfig(key);
        return config != null ? ResponseEntity.ok(config) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemConfig> saveConfig(@Valid @RequestBody SystemConfigRequest request) {
        return ResponseEntity.ok(service.saveConfig(request));
    }
}
