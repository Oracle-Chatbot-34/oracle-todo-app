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
