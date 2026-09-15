package com.scse.curriculum.auditlog.service;

import com.scse.curriculum.auditlog.dto.AuditLogResponse;
import com.scse.curriculum.auditlog.entity.AuditAction;
import com.scse.curriculum.auditlog.entity.AuditLog;
import com.scse.curriculum.auditlog.repository.AuditLogRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
import com.scse.curriculum.auditlog.entity.AuditActorSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditLogServiceImpl service;

    private AuditActorSnapshot instructorActor;

    @BeforeEach
void setUp() {
    instructorActor =
            AuditActorSnapshot.builder()
                    .id(42)
                    .username("nvminh")
                    .role("INSTRUCTOR")
                    .actorType("BUSINESS_USER")
                    .capturedAt(LocalDateTime.now())
                    .build();
}

    @Test
    void getById_shouldMapActorCorrectly_whenActorPresent() {
        AuditLog log = AuditLog.builder()
                .id(1L)
                .tableName("syllabus")
                .recordId(10)
                .action(AuditAction.UPDATE)
                .oldValue("{\"status\":\"DRAFT\"}")
                .newValue("{\"status\":\"SUBMITTED\"}")
                .changedBy(instructorActor)
                .changedAt(LocalDateTime.now())
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(log));

        AuditLogResponse response = service.getById(1L);

        // FR-01.7: actor phai la nguoi THUC SU thao tac (id=42, username=nvminh),
        // khong con bi mac dinh ve Admin nhu bug cu.
        assertThat(response.getChangedById()).isEqualTo(42);
        assertThat(response.getChangedByUsername()).isEqualTo("nvminh");
        assertThat(response.getOldValue()).contains("DRAFT");
        assertThat(response.getNewValue()).contains("SUBMITTED");
    }

    @Test
void getById_shouldPreserveLegacySystemActorFromSnapshot() {

    AuditActorSnapshot legacySystemActor =
            AuditActorSnapshot.builder()
                    .id(20)
                    .username("system")
                    .role("ADMIN")
                    .actorType("LEGACY_SYSTEM")
                    .capturedAt(LocalDateTime.now())
                    .build();

    AuditLog systemLog =
            AuditLog.builder()
                    .id(2L)
                    .tableName("syllabus")
                    .recordId(11)
                    .action(AuditAction.STATUS_CHANGE)
                    .changedBy(legacySystemActor)
                    .changedAt(LocalDateTime.now())
                    .build();

    when(repository.findById(2L))
            .thenReturn(
                    Optional.of(systemLog));

    AuditLogResponse response =
            service.getById(2L);

    assertThat(response.getChangedById())
            .isEqualTo(20);

    assertThat(response.getChangedByUsername())
            .isEqualTo("system");
}
    @Test
    void getById_shouldThrow_whenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAll_shouldReturnMappedList() {
        AuditLog log = AuditLog.builder()
                .id(1L).tableName("course").recordId(5)
                .action(AuditAction.CREATE).changedBy(instructorActor)
                .changedAt(LocalDateTime.now()).build();

        when(repository.findAll()).thenReturn(List.of(log));

        List<AuditLogResponse> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTableName()).isEqualTo("course");
    }

    @Test
    void search_shouldTrimKeywordAndDelegateToRepository() {
        when(repository.findByTableNameContainingIgnoreCaseOrChangedBy_UsernameContainingIgnoreCase("syllabus", "syllabus"))
                .thenReturn(List.of());

        List<AuditLogResponse> result = service.search("  syllabus  ");

        assertThat(result).isEmpty();
    }
}
