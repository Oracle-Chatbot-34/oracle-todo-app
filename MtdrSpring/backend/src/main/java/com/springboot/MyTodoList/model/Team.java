package com.springboot.MyTodoList.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a team in the DashMaster system.
 * Each team has exactly one manager and multiple team members.
 */
@Entity
@Table(name = "TEAMS")
public class Team {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "NAME", nullable = false)
    private String name;
    
    @Column(name = "DESCRIPTION")
    private String description;
    
    /**
     * The team manager (one user with MANAGER role)
     */
    @OneToOne
    @JoinColumn(name = "MANAGER_ID")
    private User manager;
    
    @Column(name = "CREATED_AT")
    private OffsetDateTime createdAt;
    
    /**
     * All team members (including the manager)
     * This is the inverse of the user.team relationship
     */
    @OneToMany(mappedBy = "team")
    private List<User> members = new ArrayList<>();
    
    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getManager() {
        return manager;
    }

    public void setManager(User manager) {
        this.manager = manager;
        
        // If we set a manager, that user should also manage this team
        if (manager != null) {
            manager.setManagedTeam(this);
        }
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<User> getMembers() {
        return members;
    }

    public void setMembers(List<User> members) {
        this.members = members;
    }
    
    /**
     * Sets creation timestamp before persisting a new entity
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
    
    /**
     * Adds a member to the team
     */
    public void addMember(User user) {
        if (!members.contains(user)) {
            members.add(user);
            user.setTeam(this);
        }
    }
    
    /**
     * Removes a member from the team
     */
    public void removeMember(User user) {
        if (members.contains(user)) {
            members.remove(user);
            user.setTeam(null);
        }
    }
    
    /**
     * Get the number of team members (excluding the manager)
     */
    public int getMemberCount() {
        return (int) members.stream()
            .filter(user -> !"MANAGER".equals(user.getRole()))
            .count();
    }
    
    /**
     * Get only the developers from the team
     */
    public List<User> getDevelopers() {
        return members.stream()
            .filter(User::isDeveloper)
            .toList();
    }
    
    /**
     * Get only the employees from the team
     */
    public List<User> getEmployees() {
        return members.stream()
            .filter(User::isEmployee)
            .toList();
    }
}