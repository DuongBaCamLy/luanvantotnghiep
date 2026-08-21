package com.scse.curriculum.email.dto;

public record EmailOutboxSummaryResponse(
        long pending,
        long retry,
        long sent,
        long failed,
        long total) {
}
