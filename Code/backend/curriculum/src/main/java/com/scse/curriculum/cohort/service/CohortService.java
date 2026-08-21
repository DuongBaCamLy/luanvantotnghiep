package com.scse.curriculum.cohort.service;

import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.cohort.dto.CohortResponse;

import java.util.List;

public interface CohortService {

    CohortResponse create(
            CreateCohortRequest request);

    List<CohortResponse> getAll();

    CohortResponse getById(
            Integer id);

    CohortResponse getByName(
            String name);

    List<CohortResponse> getByProgram(
            Integer programId);
}
