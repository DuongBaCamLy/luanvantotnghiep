package com.scse.curriculum.user.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByUsernameIgnoreCaseOrEmailIgnoreCase(
            String username,
            String email);

    Optional<UserAccount> findByInstructorId(Integer instructorId);

    List<UserAccount> findByInstructorIdIn(Collection<Integer> instructorIds);

    List<UserAccount> findByRole(UserRole role);

    List<UserAccount> findByRoleAndIsActiveTrue(UserRole role);

    List<UserAccount> findByRoleAndManagedMajor_IdAndIsActiveTrue(
            UserRole role, Integer managedMajorId);

    boolean existsByRoleAndManagedMajor_IdAndIsActiveTrueAndIdNot(
            UserRole role, Integer managedMajorId, Integer excludedId);

    @Query("""
            SELECT u
            FROM UserAccount u, Instructor i
            WHERE u.instructorId = i.id
              AND u.role = :role
              AND u.isActive = true
              AND i.isActive = true
              AND i.department.id = :departmentId
            ORDER BY u.id
            """)
    List<UserAccount> findActiveByRoleAndInstructorDepartmentId(
            @Param("role") UserRole role,
            @Param("departmentId") Integer departmentId);
}
