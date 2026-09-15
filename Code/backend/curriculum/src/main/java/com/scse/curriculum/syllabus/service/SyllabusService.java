package com.scse.curriculum.syllabus.service;

import com.scse.curriculum.syllabus.dto.CloneSyllabusRequest;
import com.scse.curriculum.syllabus.dto.CreateSyllabusRequest;
import com.scse.curriculum.syllabus.dto.SyllabusDiffResponse;
import com.scse.curriculum.syllabus.dto.SyllabusResponse;
import com.scse.curriculum.syllabus.dto.SyllabusCreateContextResponse;
import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;
import com.scse.curriculum.syllabus.comparison.dto.SemanticSyllabusDiffResponse;

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
SyllabusResponse getPreviousComparable(
        Integer id);
    SyllabusResponse update(
            Integer id,
            CreateSyllabusRequest request);

    void delete(
            Integer id);

    SubmissionValidationResponse validateForSubmit(
            Integer id);

    SyllabusResponse submit(
            Integer id);
SyllabusResponse createRevisionDraftFromRejected(
        Integer sourceId);
    List<SyllabusResponse> getByStatus(
            String status);

    SyllabusDiffResponse getDiff(
            Integer oldId,
            Integer newId);

            SemanticSyllabusDiffResponse getSemanticDiff(
        Integer oldId,
        Integer newId);
    SyllabusResponse clone(
            Integer id,
            CloneSyllabusRequest request);

}
