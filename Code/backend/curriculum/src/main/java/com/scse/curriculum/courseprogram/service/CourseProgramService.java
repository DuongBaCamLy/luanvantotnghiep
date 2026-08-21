package com.scse.curriculum.courseprogram.service;

import java.util.List;

import com.scse.curriculum.courseprogram.dto.CourseProgramResponse;
import com.scse.curriculum.courseprogram.dto.CreateCourseProgramRequest;

public interface CourseProgramService {

    CourseProgramResponse create(
            CreateCourseProgramRequest request);

    CourseProgramResponse getById(
            Integer id);

    List<CourseProgramResponse> getAll();

    List<CourseProgramResponse> getByProgram(
            Integer programId);

    List<CourseProgramResponse> getByCohort(
            Integer cohortId);

    List<CourseProgramResponse> getByProgramAndCohort(
            Integer programId,
            Integer cohortId);

    void delete(
            Integer id);

    CourseProgramResponse update(
            Integer id,
            com.scse.curriculum.courseprogram.dto.UpdateCourseProgramRequest request);

    CourseProgramResponse assignSyllabus(
            Integer courseProgramId,
            Integer syllabusId);
}
