package com.scse.curriculum.curriculum.service;

import com.scse.curriculum.cohort.dto.CohortResponse;
import com.scse.curriculum.cohort.dto.CreateCohortRequest;
import com.scse.curriculum.cohort.service.CohortService;
import com.scse.curriculum.curriculum.dto.CreateCurriculumRequest;
import com.scse.curriculum.curriculum.dto.CreateCurriculumResponse;
import com.scse.curriculum.program.dto.CloneProgramResponse;
import com.scse.curriculum.program.dto.ProgramResponse;
import com.scse.curriculum.program.service.ProgramService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurriculumCreationServiceImpl implements CurriculumCreationService {

    private final ProgramService programService;
    private final CohortService cohortService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateCurriculumResponse create(CreateCurriculumRequest request) {
        ProgramResponse program = programService.create(request.getProgram());

        CreateCohortRequest cohortRequest = new CreateCohortRequest();
        cohortRequest.setProgramId(program.getId());
        cohortRequest.setEntryYear(request.getCohort().getEntryYear());
        cohortRequest.setName(request.getCohort().getName());
        cohortRequest.setDescription(request.getCohort().getDescription());
        CohortResponse cohort = cohortService.create(cohortRequest);

        CloneProgramResponse clone = null;
        if (request.getCloneSource() != null) {
            clone = programService.cloneFromCohort(
                    request.getCloneSource().getProgramId(),
                    request.getCloneSource().getCohortId(),
                    program.getId(),
                    cohort.getId());
        }

        return CreateCurriculumResponse.builder()
                .program(program)
                .cohort(cohort)
                .clone(clone)
                .message(clone == null
                        ? "Curriculum created successfully"
                        : "Curriculum created and curriculum data cloned successfully")
                .build();
    }
}
