package com.scse.curriculum.auditlog.service;

import com.scse.curriculum.auditlog.entity.AuditLog;
import com.scse.curriculum.auditlog.entity.AuditActorSnapshot;
import com.scse.curriculum.auditlog.repository.AuditLogRepository;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import com.scse.curriculum.auditlog.entity.AuditAction;
import static org.assertj.core.api.Assertions.assertThat;

/** Validates HQL against mapped entities without opening any database connection. */
class AuditLogQueryContractTest {
    @Test
    void pagedQueryAndCountQueryAreValidForActorSnapshotAndEnumActions() throws Exception {
        Query query = AuditLogRepository.class.getMethod("searchPage", String.class, AuditAction.class, Pageable.class)
                .getAnnotation(Query.class);
        try (var factory = new Configuration().addAnnotatedClass(AuditLog.class)
                .addAnnotatedClass(AuditActorSnapshot.class)
                .setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .setProperty("hibernate.boot.allow_jdbc_metadata_access", "false")
                .setProperty("hibernate.hbm2ddl.auto", "none").buildSessionFactory();
             var session = factory.openSession()) {
            var content = session.createQuery(query.value(), AuditLog.class);
            content.setParameter("pattern", "%status_change%");
            content.setParameter("action", AuditAction.STATUS_CHANGE);
            var count = session.createQuery(query.countQuery(), Long.class);
            count.setParameter("pattern", "%%"); count.setParameter("action", null);
            assertThat(content.getParameter("action").getParameterType()).isEqualTo(AuditAction.class);
            assertThat(query.value()).contains("left join", "cast(log.action as string)", "escape '!'");
        }
    }
}
