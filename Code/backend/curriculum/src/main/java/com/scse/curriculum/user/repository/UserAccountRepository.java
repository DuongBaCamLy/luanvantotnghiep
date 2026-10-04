package com.scse.curriculum.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.entity.UserRole;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, Integer> {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByUsernameIgnoreCaseOrEmailIgnoreCase(
            String username,
            String email);

    List<UserAccount> findByRole(UserRole role);

    List<UserAccount> findByRoleAndIsActiveTrue(
            UserRole role);

    List<UserAccount> findByRoleAndManagedMajor_IdAndIsActiveTrue(
            UserRole role,
            Integer managedMajorId);

    boolean existsByRoleAndManagedMajor_IdAndIsActiveTrueAndIdNot(
            UserRole role,
            Integer managedMajorId,
            Integer excludedId);
}