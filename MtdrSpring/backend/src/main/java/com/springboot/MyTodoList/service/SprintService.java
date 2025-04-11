package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.repository.SprintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for sprint management operations.
 */
@Service
public class SprintService {
    @Autowired
    private SprintRepository sprintRepository;

    /**
     * Find all sprints
     */
    public List<Sprint> findAll() {
        return sprintRepository.findAll();
    }

    /**
     * Find a sprint by ID
     */
    public Optional<Sprint> findById(Long id) {
        return sprintRepository.findById(id);
    }

    /**
     * Find all sprints for a team
     */
    public List<Sprint> findByTeamId(Long teamId) {
        return sprintRepository.findByTeamId(teamId);
    }

    /**
     * Find the active sprint for a team
     */
    public Optional<Sprint> findActiveByTeamId(Long teamId) {
        return sprintRepository.findByTeamIdAndStatus(teamId, "ACTIVE");
    }

    /**
     * Create a new sprint
     */
    @Transactional
    public Sprint save(Sprint sprint) {
        if (sprint.getCreatedAt() == null) {
            sprint.setCreatedAt(OffsetDateTime.now());
        }
        sprint.setUpdatedAt(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }

    /**
     * Update an existing sprint
     */
    @Transactional
    public Sprint update(Sprint sprint) {
        sprint.setUpdatedAt(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }

    /**
     * Start a sprint
     */
    @Transactional
    public Sprint startSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found with ID: " + sprintId));
        sprint.setStatus("ACTIVE");
        sprint.setStartDate(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }

    /**
     * Complete a sprint
     */
    @Transactional
    public Sprint completeSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found with ID: " + sprintId));
        sprint.setStatus("COMPLETED");
        return sprintRepository.save(sprint);
    }

    /**
     * Delete a sprint
     */
    @Transactional
    public void deleteSprint(Long id) {
        sprintRepository.deleteById(id);
    }

    /**
     * Find completed sprints by team ID
     */
    public List<Sprint> findCompletedByTeamId(Long teamId) {
        // Since we don't have findByTeamIdAndStatusNot, let's use findByTeamId and
        // filter the results
        List<Sprint> allTeamSprints = sprintRepository.findByTeamId(teamId);
        return allTeamSprints.stream()
                .filter(sprint -> !"ACTIVE".equals(sprint.getStatus()))
                .toList();
    }

    /**
     * Find Active sprints by team ID
     */
    
}