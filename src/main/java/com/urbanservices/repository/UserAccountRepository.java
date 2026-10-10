package com.urbanservices.repository;

import com.urbanservices.model.UserAccount;
import com.urbanservices.model.UserRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    long countByRole(UserRole role);
    List<UserAccount> findAllByRoleOrderByNameAsc(UserRole role);
}
