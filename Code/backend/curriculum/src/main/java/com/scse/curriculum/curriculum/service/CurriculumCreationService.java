package com.scse.curriculum.curriculum.service;

import com.scse.curriculum.curriculum.dto.CreateCurriculumRequest;
import com.scse.curriculum.curriculum.dto.CreateCurriculumResponse;

public interface CurriculumCreationService {
    CreateCurriculumResponse create(CreateCurriculumRequest request);
}
