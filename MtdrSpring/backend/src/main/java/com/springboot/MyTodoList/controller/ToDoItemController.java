package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.ToDoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
public class ToDoItemController {
    @Autowired
    private ToDoItemService toDoItemService;

    @GetMapping
    public ResponseEntity<List<ToDoItem>> getAllToDoItems() {
        return ResponseEntity.ok(toDoItemService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ToDoItem> getToDoItemById(@PathVariable int id) {
        Optional<ToDoItem> todoData = toDoItemService.findById(id);
        return todoData.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody ToDoItem todoItem) {
        try {
            ToDoItem task = toDoItemService.addTaskWithEstimation(todoItem);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("location", String.valueOf(task.getId()))
                    .body(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable int id, @RequestBody ToDoItem todoItem) {
        try {
            todoItem.setId(id);
            ToDoItem updatedTask = toDoItemService.updateToDoItem(id, todoItem);
            if (updatedTask == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updatedTask);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable int id) {
        try {
            boolean deleted = toDoItemService.deleteToDoItem(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/assign-to-sprint/{sprintId}")
    public ResponseEntity<?> assignTaskToSprint(@PathVariable("id") int id, @PathVariable("sprintId") Long sprintId) {
        try {
            ToDoItem task = toDoItemService.assignTaskToSprint(id, sprintId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<?> startTask(@PathVariable("id") int id, @RequestParam("userId") Long userId) {
        try {
            ToDoItem task = toDoItemService.startTask(id, userId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(
            @PathVariable("id") int id,
            @RequestParam("actualHours") Double actualHours,
            @RequestParam(value = "comments", required = false) String comments) {
        try {
            ToDoItem task = toDoItemService.completeTask(id, actualHours, comments != null ? comments : "");
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<?> getTasksBySprintId(@PathVariable("sprintId") Long sprintId) {
        try {
            List<ToDoItem> tasks = toDoItemService.findTasksBySprintId(sprintId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActiveTasksByUserId(@PathVariable("userId") Long userId) {
        try {
            List<ToDoItem> tasks = toDoItemService.findActiveTasksByAssigneeId(userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "An error occurred: " + e.getMessage()));
        }
    }
}