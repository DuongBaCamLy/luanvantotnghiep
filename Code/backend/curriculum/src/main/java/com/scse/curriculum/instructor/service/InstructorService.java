package com.scse.curriculum.instructor.service;

import java.util.List;

import com.scse.curriculum.instructor.dto.InstructorRequest;
import com.scse.curriculum.instructor.dto.InstructorResponse;

public interface InstructorService {

    InstructorResponse createInstructor(InstructorRequest request);

    List<InstructorResponse> getAllInstructors();

    InstructorResponse updateInstructor(Integer id, InstructorRequest request);

    void deleteInstructor(Integer id);

    InstructorResponse getInstructorById(Integer id);

    InstructorResponse getInstructorByUserId(Integer userId);

}