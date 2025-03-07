package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for user management operations.
 */
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Find all users in the system
     */
    public List<User> findAll() {
        return userRepository.findAll();
    }

    /**
     * Find a user by their ID
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Find a user by their Telegram ID
     */
    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    /**
     * Find a user by their username
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Find a user by their employee ID
     */
    public Optional<User> findByEmployeeId(String employeeId) {
        return userRepository.findByEmployeeId(employeeId);
    }

    /**
     * Find all users with a specific role
     */
    public List<User> findByRole(String role) {
        return userRepository.findByRole(role);
    }

    /**
     * Find all users in a specific team
     */
    public List<User> findByTeamId(Long teamId) {
        return userRepository.findByTeamId(teamId);
    }

    /**
     * Find the manager of a team
     */
    public Optional<User> findManagerByTeamId(Long teamId) {
        return userRepository.findByManagedTeamId(teamId);
    }

    /**
     * Create a new user
     */
    @Transactional
    public User createUser(User user) {
        // Encode password for security
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set timestamps
        OffsetDateTime now = OffsetDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userRepository.save(user);
    }

    /**
     * Update an existing user
     */
    @Transactional
    public User updateUser(User user) {
        // Set update timestamp
        user.setUpdatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Delete a user by ID
     */
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Authenticate an employee using their employeeId
     * Used for Telegram bot authentication
     */
    public boolean authenticateEmployee(String employeeId) {
        Optional<User> userOpt = userRepository.findByEmployeeId(employeeId);

        // Verify that the user exists and has employee or developer role
        return userOpt.map(user -> user.isEmployee() || user.isDeveloper()).orElse(false);
    }
}