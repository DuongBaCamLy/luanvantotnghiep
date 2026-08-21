package com.scse.curriculum.email;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository
        extends JpaRepository<EmailOutbox, Long> {

    @Query("""
            SELECT e.id
            FROM EmailOutbox e
            WHERE e.status IN :statuses
              AND e.nextAttemptAt <= :now
            ORDER BY e.createdAt ASC
            """)
    List<Long> findReadyIds(
            @Param("statuses") Collection<EmailDeliveryStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    Page<EmailOutbox> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<EmailOutbox> findByStatusOrderByCreatedAtDesc(
            EmailDeliveryStatus status,
            Pageable pageable);

    long countByStatus(EmailDeliveryStatus status);
}
