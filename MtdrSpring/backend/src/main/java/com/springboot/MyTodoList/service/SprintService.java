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
    public Optional<Sprint> findActiveSprintByTeamId(Long teamId) {
        return sprintRepository.findByTeamIdAndStatus(teamId, "ACTIVE");
    }
    
    /**
     * Create a new sprint
     */
    @Transactional
    public Sprint createSprint(Sprint sprint) {
        sprint.setCreatedAt(OffsetDateTime.now());
        sprint.setUpdatedAt(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }
    
    /**
     * Update an existing sprint
     */
    @Transactional
    public Sprint updateSprint(Sprint sprint) {
        sprint.setUpdatedAt(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }
    
    /**
     * Start a sprint
     */
    @Transactional
    public Sprint startSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId).orElseThrow();
        sprint.setStatus("ACTIVE");
        sprint.setStartDate(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }
    
    /**
     * Complete a sprint
     */
    @Transactional
    public Sprint completeSprint(Long sprintId) {
        Sprint sprint = sprintRepository.findById(sprintId).orElseThrow();
        sprint.setStatus("COMPLETED");
        sprint.setEndDate(OffsetDateTime.now());
        return sprintRepository.save(sprint);
    }
    
    /**
     * Delete a sprint
     */
    @Transactional
    public void deleteSprint(Long id) {
        sprintRepository.deleteById(id);
    }
}