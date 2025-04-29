package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.service.SprintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    @Autowired
    private SprintService sprintService;

    @GetMapping
    public ResponseEntity<List<Sprint>> getAllSprints() {
        List<Sprint> sprints = sprintService.findAll();
        return ResponseEntity.ok(sprints);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSprintById(@PathVariable("id") Long id) {
        return sprintService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Sprint not found with ID: " + id)));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Sprint>> getSprintsByTeamId(@PathVariable("teamId") Long teamId) {
        List<Sprint> sprints = sprintService.findByTeamId(teamId);
        return ResponseEntity.ok(sprints);
    }

    @GetMapping("/team/{teamId}/active")
    public ResponseEntity<?> getActiveSprintByTeamId(@PathVariable("teamId") Long teamId) {
        return sprintService.findActiveSprintByTeamId(teamId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No active sprint found for team ID: " + teamId)));
    }

    @PostMapping
    public ResponseEntity<?> createSprint(@RequestBody Sprint sprint) {
        try {
            Sprint createdSprint = sprintService.createSprint(sprint);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSprint(@PathVariable("id") Long id, @RequestBody Sprint sprint) {
        try {
            if (!id.equals(sprint.getId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sprint ID in path does not match ID in body"));
            }

            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Sprint not found with ID: " + id));
            }

            Sprint updatedSprint = sprintService.updateSprint(sprint);
            return ResponseEntity.ok(updatedSprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.startSprint(id);
            return ResponseEntity.ok(sprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.completeSprint(id);
            return ResponseEntity.ok(sprint);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSprint(@PathVariable("id") Long id) {
        try {
            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Sprint not found with ID: " + id));
            }

            sprintService.deleteSprint(id);
            return ResponseEntity.ok(Map.of("message", "Sprint deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
}