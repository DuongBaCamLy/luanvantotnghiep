package com.scse.curriculum.curriculummap.service;

import com.scse.curriculum.curriculummap.dto.CurriculumMapResponse;

public interface CurriculumMapService {

    CurriculumMapResponse generate(
        Integer programId,
        Integer cohortId,
        String semester,
        String status);
}
