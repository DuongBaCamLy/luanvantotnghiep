package com.scse.curriculum.syllabus.exception;

import com.scse.curriculum.syllabus.dto.SubmissionValidationResponse;

import lombok.Getter;

@Getter
public class SyllabusSubmissionValidationException extends RuntimeException {

    private final SubmissionValidationResponse validation;

    public SyllabusSubmissionValidationException(
            SubmissionValidationResponse validation) {

        super(validation != null && validation.getMessage() != null
                ? validation.getMessage()
                : "ềề cương chưa đủ điều kiện để nộp.");
        this.validation = validation;
    }
}
