package com.scse.curriculum.approval.service;


import java.util.List;


import com.scse.curriculum.approval.dto.ApprovalHistoryResponse;
import com.scse.curriculum.approval.dto.ApprovalResponse;
import com.scse.curriculum.approval.dto.ReviewApprovalRequest;
import com.scse.curriculum.approval.entity.ApprovalStep;



public interface ApprovalRequestService {



    ApprovalResponse review(
            Integer id,
            ReviewApprovalRequest request);



    ApprovalResponse getById(
            Integer id);



    List<ApprovalResponse> getAll();



    List<ApprovalResponse> getPendingByStep(
            ApprovalStep step);



    List<ApprovalResponse> getBySyllabus(
            Integer syllabusId);



    /**
     * FR-05.8
     * Xem lịch sử review/comment của syllabus
     *
     * Bao gồm:
     * - bước phê duyệt
     * - trạng thái
     * - người gửi
     * - người review
     * - comment
     * - thời gian tạo
     * - thời gian xử lý
     */
    List<ApprovalHistoryResponse> getApprovalHistory(
            Integer syllabusId);


}