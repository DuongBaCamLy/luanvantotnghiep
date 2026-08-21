package com.scse.curriculum.auditlog.repository;

import com.scse.curriculum.auditlog.entity.AuditLog;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

/**
 * FR-01.7: Repository CHỈ ĐỌC cho AuditLog.
 *
 * Cố ý KHÔNG kế thừa JpaRepository/CrudRepository: chỉ khai báo đúng các method
 * đọc/tìm kiếm cần dùng. Điều này đảm bảo tính bất biến (immutability) của nhật ký
 * ngay ở compile-time — không một đoạn code Java nào trong dự án (hiện tại hoặc
 * tương lai) có thể gọi save()/delete()/deleteById() trên audit_log, vì các
 * phương thức đó không tồn tại trên interface này.
 *
 * Việc ghi log DUY NHẤT được thực hiện bởi AuditLogAsyncProcessor bằng một câu
 * INSERT thô qua JdbcTemplate (không đi qua repository này).
 */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    Optional<AuditLog> findById(Long id);

    List<AuditLog> findAll();

    List<AuditLog> findAllByOrderByChangedAtDesc(Pageable pageable);

    List<AuditLog> findByTableNameOrderByChangedAtDesc(String tableName);

    List<AuditLog> findByTableNameContainingIgnoreCaseOrChangedBy_UsernameContainingIgnoreCase(
            String tableName,
            String username);
}
