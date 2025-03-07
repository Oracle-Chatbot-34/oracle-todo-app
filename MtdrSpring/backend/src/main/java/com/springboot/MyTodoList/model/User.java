package com.springboot.MyTodoList.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Represents a user in the DashMaster system.
 * Users can be managers, developers, or employees with different access rights.
 * Each user belongs to exactly one team.
 */
@Entity
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TELEGRAM_ID")
    private Long telegramId;

    @Column(name = "USERNAME", nullable = false, unique = true)
    private String username;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "EMPLOYEE_ID", unique = true)
    private String employeeId;

    /**
     * Role can be one of: MANAGER, DEVELOPER, EMPLOYEE
     * - MANAGER: Can view reports, manage team, and assign tasks
     * - DEVELOPER: Can update task status and create new tasks
     * - EMPLOYEE: Limited access, mainly through Telegram bot
     */
    @Column(name = "ROLE", nullable = false)
    private String role;

    @Column(name = "FULL_NAME", nullable = false)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEAM_ID")
    private Team team;

    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    /**
     * Link to the team this user manages (if they are a manager)
     * This is the inverse of the team.manager relationship
     */
    @OneToOne(mappedBy = "manager")
    private Team managedTeam;

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Team getManagedTeam() {
        return managedTeam;
    }

    public void setManagedTeam(Team managedTeam) {
        this.managedTeam = managedTeam;
    }

    /**
     * Checks if the user has a manager role
     */
    public boolean isManager() {
        return "MANAGER".equals(role);
    }

    /**
     * Checks if the user has a developer role
     */
    public boolean isDeveloper() {
        return "DEVELOPER".equals(role);
    }

    /**
     * Checks if the user has an employee role
     */
    public boolean isEmployee() {
        return "EMPLOYEE".equals(role);
    }

    /**
     * Sets creation timestamp before persisting a new entity
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    /**
     * Updates the timestamp on entity update
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}