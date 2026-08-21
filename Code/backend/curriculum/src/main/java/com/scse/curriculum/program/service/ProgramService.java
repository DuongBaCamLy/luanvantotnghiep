package com.scse.curriculum.program.service;

import java.util.List;

import com.scse.curriculum.program.dto.CloneProgramRequest;
import com.scse.curriculum.program.dto.CloneProgramResponse;
import com.scse.curriculum.program.dto.CreateProgramRequest;
import com.scse.curriculum.program.dto.CurriculumTimelineResponse;
import com.scse.curriculum.program.dto.ProgramArchiveValidationResponse;
import com.scse.curriculum.program.dto.ProgramCreditValidationResponse;
import com.scse.curriculum.program.dto.ProgramDiffResponse;
import com.scse.curriculum.program.dto.ProgramResponse;
import com.scse.curriculum.program.dto.UpdateProgramRequest;

public interface ProgramService {

    ProgramResponse create(
            CreateProgramRequest request);

    List<ProgramResponse> getAll();

    ProgramResponse getById(
            Integer id);

    ProgramResponse getByCode(
            String code);

    ProgramResponse update(
            Integer id,
            UpdateProgramRequest request);

    ProgramArchiveValidationResponse validateArchive(
            Integer id);

    ProgramResponse archive(
            Integer id);

    ProgramResponse reactivate(
            Integer id);

    ProgramDiffResponse getDiff(
            Integer programId,
            Integer oldCohortId,
            Integer newCohortId);
List<CurriculumTimelineResponse> getCurriculumTimeline(
        Integer programId);
    ProgramCreditValidationResponse validateCredits(
            Integer programId,
            Integer cohortId);

    CloneProgramResponse cloneToCohort(
            Integer programId,
            CloneProgramRequest request);

    CloneProgramResponse cloneFromCohort(
            Integer sourceProgramId,
            Integer sourceCohortId,
            Integer targetProgramId,
            Integer targetCohortId);
}