package com.scse.curriculum.approval.entity;

public enum ApprovalStep {

    STEP1_DEPT_HEAD,

    // Legacy value. Kept for historical approval records only.
    STEP2_PROG_COORDINATOR,

    // Legacy pending requests are reviewed directly by either final reviewer.
    STEP2_DEAN_SECRETARY,

    // Shared final approval queue for DEAN and DEAN_SECRETARY.
    STEP3_DEAN
}
