package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.service.ToDoItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ToDoItemController {
    @Autowired
    private ToDoItemService toDoItemService;

    // @CrossOrigin
    @GetMapping(value = "/todolist")
    public List<ToDoItem> getAllToDoItems() {
        return toDoItemService.findAll();
    }

    // @CrossOrigin
    @GetMapping(value = "/todolist/{id}")
    public ResponseEntity<ToDoItem> getToDoItemById(@PathVariable int id) {
        try {
            ResponseEntity<ToDoItem> responseEntity = toDoItemService.getItemById(id);
            return new ResponseEntity<ToDoItem>(responseEntity.getBody(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // @CrossOrigin
    @PostMapping(value = "/todolist")
    public ResponseEntity addToDoItem(@RequestBody ToDoItem todoItem) throws Exception {
        ToDoItem td = toDoItemService.addToDoItem(todoItem);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("location", "" + td.getID());
        responseHeaders.set("Access-Control-Expose-Headers", "location");
        // URI location = URI.create(""+td.getID())

        return ResponseEntity.ok()
                .headers(responseHeaders).build();
    }

    /**
     * Create a new task with estimation validation
     */
    @PostMapping(value = "/tasks")
    public ResponseEntity<?> addTaskWithEstimation(@RequestBody ToDoItem todoItem) {
        try {
            ToDoItem td = toDoItemService.addTaskWithEstimation(todoItem);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("location", "" + td.getID());
            responseHeaders.set("Access-Control-Expose-Headers", "location");

            return ResponseEntity.ok()
                    .headers(responseHeaders).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Assign a task to a sprint
     */
    @PostMapping(value = "/tasks/{id}/assign-to-sprint/{sprintId}")
    public ResponseEntity<?> assignTaskToSprint(@PathVariable("id") int id, @PathVariable("sprintId") Long sprintId) {
        try {
            ToDoItem task = toDoItemService.assignTaskToSprint(id, sprintId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Start working on a task
     */
    @PostMapping(value = "/tasks/{id}/start")
    public ResponseEntity<?> startTask(@PathVariable("id") int id, @RequestParam("userId") Long userId) {
        try {
            ToDoItem task = toDoItemService.startTask(id, userId);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Complete a task
     */
    @PostMapping(value = "/tasks/{id}/complete")
    public ResponseEntity<?> completeTask(
            @PathVariable("id") int id,
            @RequestParam("actualHours") Double actualHours,
            @RequestParam(value = "comments", required = false) String comments) {
        try {
            if (comments == null) {
                comments = "";
            }
            ToDoItem task = toDoItemService.completeTask(id, actualHours, comments);
            return ResponseEntity.ok(task);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Get all tasks in a sprint
     */
    @GetMapping(value = "/sprints/{sprintId}/tasks")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ToDoItem>> getTasksBySprintId(@PathVariable("sprintId") Long sprintId) {
        try {
            List<ToDoItem> tasks = toDoItemService.findTasksBySprintId(sprintId);

            // Add cache control headers
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Get active tasks for a user
     */
    @GetMapping(value = "/users/{userId}/active-tasks")
    public ResponseEntity<List<ToDoItem>> getActiveTasksByUserId(@PathVariable("userId") Long userId) {
        try {
            List<ToDoItem> tasks = toDoItemService.findActiveTasksByAssigneeId(userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // @CrossOrigin
    @PutMapping(value = "todolist/{id}")
    public ResponseEntity updateToDoItem(@RequestBody ToDoItem toDoItem, @PathVariable int id) {
        try {
            ToDoItem toDoItem1 = toDoItemService.updateToDoItem(id, toDoItem);
            System.out.println(toDoItem1.toString());
            return new ResponseEntity<>(toDoItem1, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }

    // @CrossOrigin
    @DeleteMapping(value = "todolist/{id}")
    public ResponseEntity<Boolean> deleteToDoItem(@PathVariable("id") int id) {
        Boolean flag = false;
        try {
            flag = toDoItemService.deleteToDoItem(id);
            return new ResponseEntity<>(flag, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(flag, HttpStatus.NOT_FOUND);
        }
    }

}
