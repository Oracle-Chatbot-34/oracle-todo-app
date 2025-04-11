package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.ApiResponse;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.SprintService;
import com.springboot.MyTodoList.service.ToDoItemService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
public class SprintController {

    @Autowired
    private SprintService sprintService;

    @Autowired
    private ToDoItemService toDoItemService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Sprint>>> getAllSprints() {
        List<Sprint> sprints = sprintService.findAll();
        return ResponseEntity.ok(new ApiResponse<>(sprints));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Sprint>> getSprintById(@PathVariable("id") Long id) {
        return sprintService.findById(id)
                .map(sprint -> ResponseEntity.ok(new ApiResponse<>(sprint)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Sprint not found with ID: " + id)));
    }

    @GetMapping("/team/{teamId}")
    public ResponseEntity<ApiResponse<List<Sprint>>> getSprintsByTeamId(@PathVariable("teamId") Long teamId) {
        List<Sprint> sprints = sprintService.findByTeamId(teamId);
        return ResponseEntity.ok(new ApiResponse<>(sprints));
    }

    @GetMapping("/team/{teamId}/active")
    public ResponseEntity<ApiResponse<Sprint>> getActiveSprintByTeamId(@PathVariable("teamId") Long teamId) {
        return sprintService.findActiveByTeamId(teamId)
                .map(sprint -> ResponseEntity.ok(new ApiResponse<>(sprint)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("No active sprint found for team ID: " + teamId)));
    }

    @GetMapping("/{id}/tasks")
    public ResponseEntity<ApiResponse<List<ToDoItem>>> getTasksForSprint(@PathVariable("id") Long id) {
        try {
            List<ToDoItem> tasks = toDoItemService.findTasksBySprintId(id);
            return ResponseEntity.ok(new ApiResponse<>(tasks));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Sprint>> createSprint(@RequestBody Sprint sprint) {
        try {
            Sprint createdSprint = sprintService.save(sprint);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(createdSprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Sprint>> updateSprint(@PathVariable("id") Long id, @RequestBody Sprint sprint) {
        try {
            if (!id.equals(sprint.getId())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>("Sprint ID in path does not match ID in body"));
            }

            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Sprint not found with ID: " + id));
            }

            Sprint updatedSprint = sprintService.update(sprint);
            return ResponseEntity.ok(new ApiResponse<>(updatedSprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<Sprint>> startSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.startSprint(id);
            return ResponseEntity.ok(new ApiResponse<>(sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Sprint>> completeSprint(@PathVariable("id") Long id) {
        try {
            Sprint sprint = sprintService.completeSprint(id);
            return ResponseEntity.ok(new ApiResponse<>(sprint));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSprint(@PathVariable("id") Long id) {
        try {
            if (!sprintService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>("Sprint not found with ID: " + id));
            }

            sprintService.deleteSprint(id);
            return ResponseEntity.ok(new ApiResponse<>(null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>("An error occurred: " + e.getMessage()));
        }
    }
}