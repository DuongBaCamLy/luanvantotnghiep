package com.scse.curriculum.major.service;

import com.scse.curriculum.major.dto.CreateMajorRequest;
import com.scse.curriculum.major.dto.MajorResponse;

import java.util.List;

public interface MajorService {

    MajorResponse create(
            CreateMajorRequest request);

    List<MajorResponse> getAll();

    MajorResponse getById(Integer id);

    MajorResponse getByCode(String code);
}