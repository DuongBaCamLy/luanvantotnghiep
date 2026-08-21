package com.scse.curriculum.clo.service;

import com.scse.curriculum.clo.dto.CloResponse;
import com.scse.curriculum.clo.dto.CreateCloRequest;

import java.util.List;

public interface CloService {

    CloResponse create(CreateCloRequest request);

    List<CloResponse> getAll();

    CloResponse getById(Integer id);

    List<CloResponse> getBySyllabus(Integer syllabusId);

    CloResponse update(Integer id, CreateCloRequest request);

    void delete(Integer id);
}