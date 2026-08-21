package com.scse.curriculum.approval.dto;

import com.scse.curriculum.approval.entity.ApprovalStep;

import lombok.Data;

@Data
public class CreateApprovalRequest {

    private Integer syllabusId;

    private Integer requestedById;

    private ApprovalStep step;

}