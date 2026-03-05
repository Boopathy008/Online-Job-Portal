package com.jobportal.repository;

import com.jobportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findByRoleAndVerificationStatus(com.jobportal.entity.Role role,
            com.jobportal.entity.VerificationStatus status);
}
