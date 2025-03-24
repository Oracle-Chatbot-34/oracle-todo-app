package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Get all users with simplified response format
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userService.findAll();
        List<Map<String, Object>> simplifiedUsers = users.stream()
            .map(this::convertUserToMap)
            .toList();
        
        return ResponseEntity.ok(simplifiedUsers);
    }

    /**
     * Get user by ID with simplified response format
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable("id") Long id) {
        Optional<User> userOpt = userService.findById(id);
        if (userOpt.isPresent()) {
            Map<String, Object> userData = convertUserToMap(userOpt.get());
            return ResponseEntity.ok(userData);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
        }
    }

    /**
     * Get users by role with simplified response format
     */
    @GetMapping("/users/roles/{role}")
    public ResponseEntity<List<Map<String, Object>>> getUsersByRole(@PathVariable("role") String role) {
        List<User> users = userService.findByRole(role);
        List<Map<String, Object>> simplifiedUsers = users.stream()
            .map(this::convertUserToMap)
            .toList();
        
        return ResponseEntity.ok(simplifiedUsers);
    }

    /**
     * Create a new user
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // Check if username already exists
            if (userService.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists: " + user.getUsername());
            }

            // Check if employee ID already exists
            if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()
                    && userService.findByEmployeeId(user.getEmployeeId()).isPresent()) {
                return ResponseEntity.badRequest().body("Employee ID already exists: " + user.getEmployeeId());
            }

            // Set encoded password
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            User createdUser = userService.createUser(user);
            Map<String, Object> userData = convertUserToMap(createdUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(userData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Update a user
     */
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long id, @RequestBody User user) {
        try {
            if (!id.equals(user.getId())) {
                return ResponseEntity.badRequest().body("User ID in path does not match ID in body");
            }

            Optional<User> existingUserOpt = userService.findById(id);
            if (!existingUserOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            User existingUser = existingUserOpt.get();

            // If password is provided, encode it
            if (user.getPassword() != null && !user.getPassword().isEmpty() && !user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                // Use existing password if not provided
                user.setPassword(existingUser.getPassword());
            }

            User updatedUser = userService.updateUser(user);
            Map<String, Object> userData = convertUserToMap(updatedUser);

            return ResponseEntity.ok(userData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Delete a user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        try {
            if (!userService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with ID: " + id);
            }

            userService.deleteUser(id);
            return ResponseEntity.ok("User deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Update a user's Telegram ID
     */
    @PutMapping("/users/{id}/telegram/{telegramId}")
    public ResponseEntity<?> updateTelegramId(
            @PathVariable("id") Long id,
            @PathVariable("telegramId") Long telegramId) {
        try {
            User updatedUser = userService.updateTelegramId(id, telegramId);
            Map<String, Object> userData = convertUserToMap(updatedUser);

            return ResponseEntity.ok(userData);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to convert User to Map with simplified representation
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("username", user.getUsername());
        userData.put("fullName", user.getFullName());
        userData.put("role", user.getRole());
        userData.put("employeeId", user.getEmployeeId());
        userData.put("phoneNumber", user.getPhoneNumber());
        userData.put("telegramId", user.getTelegramId());
        userData.put("teamId", user.getTeam() != null ? user.getTeam().getId() : null);
        userData.put("createdAt", user.getCreatedAt());
        userData.put("updatedAt", user.getUpdatedAt());
        
        // Include managed team ID if the user is a manager
        if (user.getManagedTeam() != null) {
            userData.put("managedTeamId", user.getManagedTeam().getId());
        }
        
        return userData;
    }
}