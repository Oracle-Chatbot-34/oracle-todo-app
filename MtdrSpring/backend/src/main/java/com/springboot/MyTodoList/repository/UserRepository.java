package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations.
 * Provides methods for finding and managing users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Find a user by their Telegram ID
     */
    Optional<User> findByTelegramId(Long telegramId);

    /**
     * Find a user by their username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by their employee ID
     */
    Optional<User> findByEmployeeId(String employeeId);

    /**
     * Check if a username already exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if an employee ID already exists
     */
    boolean existsByEmployeeId(String employeeId);

    /**
     * Find all users with a specific role
     */
    List<User> findByRole(String role);

    /**
     * Find all users belonging to a team
     */
    List<User> findByTeamId(Long teamId);

    /**
     * Find the manager of a team
     */
    Optional<User> findByManagedTeamId(Long teamId);

    /**
     * Find developers in a team
     */
    List<User> findByTeamIdAndRole(Long teamId, String role);
}