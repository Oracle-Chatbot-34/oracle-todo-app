package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class SprintController {

    @Autowired
    private SprintService sprintService;
    
    @Autowired
    private ToDoItemService toDoItemService;
    
    /**
     * Get all sprints
     */
    @GetMapping("/sprints")
    public ResponseEntity<List<Sprint>> getAllSprints() {
        List<Sprint> sprints = sprintService.findAll();
        return ResponseEntity.ok(sprints);
    }
    
    /**
     * Get sprint by ID
     */
    @GetMapping("/sprints/{id}")
    public ResponseEntity<?> getSprintById(@PathVariable("id") Long id) {
        Optional<Sprint> sprintOpt = sprintService.findById(id);
        if (sprintOpt.isPresent()) {
            return ResponseEntity.ok(sprintOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sprint not found with ID: " + id);
        }
    }
    
    /**
     * Get sprints for a team
     */
    @GetMapping("/teams/{teamId}/sprints")
    public ResponseEntity<List<Sprint>> getSprintsByTeamId(@PathVariable("teamId") Long teamId) {
        List<Sprint> sprints = sprintService.findByTeamId(teamId);
        return ResponseEntity.ok(sprints);
    }
    
    /**
     * Get active sprint for a team
     */
    @GetMapping("/teams/{teamId}/active-sprint")
    public ResponseEntity<?> getActiveSprintByTeamId(@PathVariable("teamId") Long teamId) {
        Optional<Sprint> sprintOpt = sprintService.findActiveSprintByTeamId(teamId);
        if (sprintOpt.isPresent()) {
            return ResponseEntity.ok(sprintOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No active sprint found for team ID: " + teamId);
        }
    }
    
    /**
     * Create a new sprint
     */
    @PostMapping("/sprints")
    public ResponseEntity<?> createSprint(@RequestBody Sprint sprint) {
        try {
            Sprint createdSprint = sprintService.createSprint(sprint);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Update a sprint
     */
    @PutMapping("/sprints/{id}")
    public ResponseEntity<?> updateSprint(@PathVariable("id") Long id, @RequestBody Sprint sprint) {
        try {
            if (!id.equals(sprint.getId())) {
                return ResponseEntity.badRequest().body("Sprint ID in path does not match ID in body");
            }
            
            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sprint not found with ID: " + id);
            }
            
            Sprint updatedSprint = sprintService.updateSprint(sprint);
            return ResponseEntity.ok(updatedSprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Start a sprint
     */
    @PostMapping("/sprints/{id}/start")
    public ResponseEntity<?> startSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.startSprint(id);
            return ResponseEntity.ok(sprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Complete a sprint
     */
    @PostMapping("/sprints/{id}/complete")
    public ResponseEntity<?> completeSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.completeSprint(id);
            return ResponseEntity.ok(sprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Delete a sprint
     */
    @DeleteMapping("/sprints/{id}")
    public ResponseEntity<?> deleteSprint(@PathVariable("id") Long id) {
        try {
            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sprint not found with ID: " + id);
            }
            
            sprintService.deleteSprint(id);
            return ResponseEntity.ok("Sprint deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Get sprint board (all tasks in the sprint with their status)
     */
    @GetMapping("/sprints/{id}/board")
    public ResponseEntity<?> getSprintBoard(@PathVariable("id") Long id) {
        try {
            Optional<Sprint> sprintOpt = sprintService.findById(id);
            if (!sprintOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sprint not found with ID: " + id);
            }
            
            List<ToDoItem> tasks = toDoItemService.findTasksBySprintId(id);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}