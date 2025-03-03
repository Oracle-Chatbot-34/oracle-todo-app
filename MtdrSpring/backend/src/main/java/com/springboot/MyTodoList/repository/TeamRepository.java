package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Team entity operations.
 * Provides methods for finding and managing teams.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    /**
     * Check if a team name already exists
     */
    boolean existsByName(String name);
    
    /**
     * Find team by manager ID
     */
    Optional<Team> findByManagerId(Long managerId);
}