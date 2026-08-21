package com.scse.curriculum.approval.dto;

import com.scse.curriculum.approval.entity.ApprovalStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewApprovalRequest {

    @NotNull(message = "Vui lòng chọn quyết định phê duyệt")
    private ApprovalStatus status;

    @Size(max = 4000, message = "Nhận xét không được vượt quá 4000 ký tự")
    private String comment;
}
