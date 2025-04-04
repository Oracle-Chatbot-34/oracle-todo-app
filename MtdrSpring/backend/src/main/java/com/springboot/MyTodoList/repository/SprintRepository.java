package com.springboot.MyTodoList.repository;

import com.springboot.MyTodoList.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Sprint entity operations.
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {
    /**
     * Find all sprints for a team
     */
    List<Sprint> findByTeamId(Long teamId);

    /**
     * Find active sprint for a team
     */
    Optional<Sprint> findByTeamIdAndStatus(Long teamId, String status);
}