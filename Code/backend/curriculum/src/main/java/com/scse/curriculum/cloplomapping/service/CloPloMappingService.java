package com.scse.curriculum.cloplomapping.service;

import com.scse.curriculum.cloplomapping.dto.CreateCloPloMappingRequest;
import com.scse.curriculum.cloplomapping.dto.CloPloMappingResponse;

import java.util.List;

public interface CloPloMappingService {

    CloPloMappingResponse create(
            CreateCloPloMappingRequest request);

    List<CloPloMappingResponse> getAll();

    CloPloMappingResponse getById(
            Integer id);

    List<CloPloMappingResponse> getByClo(
            Integer cloId);

    List<CloPloMappingResponse> getByPlo(
            Integer ploId);

    void delete(
            Integer id);
}