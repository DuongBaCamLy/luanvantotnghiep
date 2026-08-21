package com.scse.curriculum.plo.service;

import com.scse.curriculum.plo.dto.CreatePloRequest;
import com.scse.curriculum.plo.dto.PloResponse;

import java.util.List;

public interface PloService {

    PloResponse create(
            CreatePloRequest request);

    List<PloResponse> getAll();

    PloResponse getById(
            Integer id);

    List<PloResponse> getByCode(String code);

    List<PloResponse> getByProgram(
            Integer programId);

    PloResponse update(Integer id, CreatePloRequest request);

    void delete(Integer id);
}