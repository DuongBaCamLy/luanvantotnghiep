package com.scse.curriculum.config.service;

import com.scse.curriculum.config.dto.SystemConfigRequest;
import com.scse.curriculum.config.entity.SystemConfig;
import com.scse.curriculum.config.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository repository;

    public List<SystemConfig> getAllConfigs() {
        return repository.findAll();
    }

    public SystemConfig getConfig(String key) {
        return repository.findById(key).orElse(null);
    }

    public SystemConfig saveConfig(SystemConfigRequest request) {
        SystemConfig config = repository.findById(request.getKey()).orElse(new SystemConfig());
        config.setKey(request.getKey());
        config.setValue(request.getValue());
        config.setDescription(request.getDescription());
        return repository.save(config);
    }
}
