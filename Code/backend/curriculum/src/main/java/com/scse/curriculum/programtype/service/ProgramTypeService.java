package com.scse.curriculum.programtype.service;

import com.scse.curriculum.programtype.dto.CreateProgramTypeRequest;
import com.scse.curriculum.programtype.dto.ProgramTypeResponse;

import java.util.List;

public interface ProgramTypeService {

    ProgramTypeResponse create(
            CreateProgramTypeRequest request);

    List<ProgramTypeResponse> getAll();

    ProgramTypeResponse getById(Integer id);

    ProgramTypeResponse getByCode(String code);
}