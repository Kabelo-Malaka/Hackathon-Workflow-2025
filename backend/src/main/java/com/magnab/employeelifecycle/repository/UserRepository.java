package com.magnab.employeelifecycle.repository;

import com.magnab.employeelifecycle.entity.User;
import com.magnab.employeelifecycle.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity.
 * Provides CRUD operations and custom queries for user management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username.
     * Used for authentication and username uniqueness checks.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email.
     * Used for email-based lookups and uniqueness validation.
     */
    Optional<User> findByEmail(String email);

    /**
     * Find all users by active status.
     * Used for filtering active/inactive users.
     */
    List<User> findByIsActive(Boolean isActive);

    /**
     * Find active users with a specific role.
     * Used for task assignment - finds eligible users for a role with load balancing.
     * Leverages composite index (role, is_active) for efficient filtering.
     */
    List<User> findByRoleAndIsActive(UserRole role, Boolean isActive);
}
