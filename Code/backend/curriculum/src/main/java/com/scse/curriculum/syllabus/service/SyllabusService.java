package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;

import java.util.List;

public interface SyllabusService {

    SyllabusResponse create(
            CreateSyllabusRequest request);

    SyllabusCreateContextResponse getCreateContext(Integer courseId);

    List<SyllabusResponse> getAll();

    SyllabusResponse getById(
            Integer id);

    SyllabusResponse getByIdForEdit(
            Integer id);

    List<SyllabusResponse> getByCourse(
            Integer courseId);

    SyllabusResponse update(
            Integer id,
            CreateSyllabusRequest request);

    void delete(
            Integer id);

    int deleteAll();

    SubmissionValidationResponse validateForSubmit(
            Integer id);

    SyllabusResponse submit(
            Integer id);

    List<SyllabusResponse> getByStatus(
            String status);

    SyllabusDiffResponse getDiff(
            Integer oldId,
            Integer newId);

    SyllabusResponse clone(
            Integer id,
            CloneSyllabusRequest request);

}
