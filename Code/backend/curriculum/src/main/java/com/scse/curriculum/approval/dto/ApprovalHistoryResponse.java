package com.scse.curriculum.approval.dto;


import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class ApprovalHistoryResponse {


    private Integer id;
    private Integer versionNumber;
    private String versionLabel;


    private String step;


    private String status;


    private String requestedByUsername;


    private String reviewedByUsername;


    private String comment;


    private LocalDateTime createdAt;


    private LocalDateTime resolvedAt;

}