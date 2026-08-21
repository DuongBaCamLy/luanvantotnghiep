package com.scse.curriculum.auditlog.service;

import com.scse.curriculum.auditlog.controller.AuditLogController;
import com.scse.curriculum.auditlog.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FR-01.7 - "Khong the sua/xoa log; chi cho phep doc/search".
 *
 * Test nay khong goi API qua HTTP, ma kiem tra truc tiep HOP DONG (contract) cua
 * cac lop lien quan bang reflection, de:
 *  - Fail ngay tai build-time neu sau nay co ai vo tinh them lai create/update/delete
 *    vao AuditLogService, AuditLogController hoac AuditLogRepository.
 *  - Chung minh AuditLogRepository khong con ke thua JpaRepository/CrudRepository
 *    (khong co save/delete/deleteById) - bat bien duoc dam bao tu tang du lieu.
 */
class AuditLogImmutabilityContractTest {

    private static final Set<String> FORBIDDEN_METHOD_NAMES = Set.of(
            "create", "update", "delete", "deleteById", "save", "saveAll", "deleteAll"
    );

    @Test
    void auditLogServiceOnlyExposesReadAndSearchMethods() {
        assertNoForbiddenMethods(AuditLogService.class);

        // Cac method doc/search phai con day du
        assertThat(hasMethod(AuditLogService.class, "getAll")).isTrue();
        assertThat(hasMethod(AuditLogService.class, "getById")).isTrue();
        assertThat(hasMethod(AuditLogService.class, "getByTableName")).isTrue();
        assertThat(hasMethod(AuditLogService.class, "search")).isTrue();
    }

    @Test
    void auditLogControllerHasNoWriteHttpMappings() {
        for (Method method : AuditLogController.class.getDeclaredMethods()) {
            boolean hasWriteMapping =
                    method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.PutMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.PatchMapping.class)
                    || method.isAnnotationPresent(org.springframework.web.bind.annotation.DeleteMapping.class);

            assertThat(hasWriteMapping)
                    .as("Method %s trong AuditLogController khong duoc phep co POST/PUT/PATCH/DELETE mapping", method.getName())
                    .isFalse();
        }

        // Phai con it nhat cac GET mapping doc/search
        long getMappingCount = Arrays.stream(AuditLogController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class))
                .count();
        assertThat(getMappingCount).isGreaterThanOrEqualTo(4);
    }

    @Test
    void auditLogRepositoryDoesNotExposeSaveOrDelete() {
        assertNoForbiddenMethods(AuditLogRepository.class);

        // Phai khong con ke thua JpaRepository/CrudRepository (nguon goc cua save/delete)
        assertThat(org.springframework.data.jpa.repository.JpaRepository.class.isAssignableFrom(AuditLogRepository.class))
                .as("AuditLogRepository khong duoc ke thua JpaRepository")
                .isFalse();
        assertThat(org.springframework.data.repository.CrudRepository.class.isAssignableFrom(AuditLogRepository.class))
                .as("AuditLogRepository khong duoc ke thua CrudRepository")
                .isFalse();
    }

    private void assertNoForbiddenMethods(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            assertThat(FORBIDDEN_METHOD_NAMES)
                    .as("Method '%s' khong duoc phep ton tai tren %s (audit log phai bat bien)",
                            method.getName(), type.getSimpleName())
                    .doesNotContain(method.getName());
        }
    }

    private boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(m -> m.getName().equals(name));
    }
}
