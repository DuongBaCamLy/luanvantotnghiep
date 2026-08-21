package com.scse.curriculum.auditlog.service;

import com.scse.curriculum.auditlog.dto.AuditLogResponse;
import com.scse.curriculum.auditlog.entity.AuditAction;
import com.scse.curriculum.auditlog.entity.AuditLog;
import com.scse.curriculum.auditlog.repository.AuditLogRepository;
import com.scse.curriculum.common.exception.ResourceNotFoundException;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;
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

    private UserAccount instructor;

    @BeforeEach
    void setUp() {
        instructor = UserAccount.builder()
                .id(42)
                .username("nvminh")
                .email("nvminh@iu.edu.vn")
                .role(UserRole.INSTRUCTOR)
                .isActive(true)
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
                .changedBy(instructor)
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
    void getById_shouldNotThrow_whenActorIsSystemJob() {
        AuditLog systemLog = AuditLog.builder()
                .id(2L)
                .tableName("syllabus")
                .recordId(11)
                .action(AuditAction.STATUS_CHANGE)
                .changedBy(null) // truong hop phong ve, thuc te DB la NOT NULL nen luon la tai khoan SYSTEM
                .changedAt(LocalDateTime.now())
                .build();

        when(repository.findById(2L)).thenReturn(Optional.of(systemLog));

        AuditLogResponse response = service.getById(2L);

        assertThat(response.getChangedById()).isNull();
        assertThat(response.getChangedByUsername()).isEqualTo("Hệ thống");
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
                .action(AuditAction.CREATE).changedBy(instructor)
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
