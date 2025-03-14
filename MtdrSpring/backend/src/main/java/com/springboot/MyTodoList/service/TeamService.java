package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.TeamRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for team management operations.
 */
@Service
public class TeamService {

    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Find all teams
     */
    public List<Team> findAll() {
        return teamRepository.findAll();
    }
    
    /**
     * Find a team by ID
     */
    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }
    
    /**
     * Find a team by its manager's ID
     */
    public Optional<Team> findByManagerId(Long managerId) {
        return teamRepository.findByManagerId(managerId);
    }
    
    /**
     * Create a new team
     */
    @Transactional
    public Team createTeam(Team team) {
        team.setCreatedAt(OffsetDateTime.now());
        
        // If a manager is assigned, verify they have a manager role
        if (team.getManager() != null) {
            User manager = team.getManager();
            if (!"MANAGER".equals(manager.getRole())) {
                manager.setRole("MANAGER");
                userRepository.save(manager);
            }
        }
        
        return teamRepository.save(team);
    }
    
    /**
     * Update an existing team
     */
    @Transactional
    public Team updateTeam(Team team) {
        // If a manager is assigned, verify they have a manager role
        if (team.getManager() != null) {
            User manager = team.getManager();
            if (!"MANAGER".equals(manager.getRole())) {
                manager.setRole("MANAGER");
                userRepository.save(manager);
            }
        }
        
        return teamRepository.save(team);
    }
    
    /**
     * Delete a team
     */
    @Transactional
    public void deleteTeam(Long id) {
        Team team = teamRepository.findById(id).orElse(null);
        if (team != null) {
            // Remove team reference from all members
            List<User> members = userRepository.findByTeamId(id);
            for (User member : members) {
                member.setTeam(null);
                userRepository.save(member);
            }
            
            // Remove manager reference
            if (team.getManager() != null) {
                User manager = team.getManager();
                manager.setManagedTeam(null);
                userRepository.save(manager);
            }
            
            // Delete the team
            teamRepository.deleteById(id);
        }
    }
    
    /**
     * Add a member to a team
     */
    @Transactional
    public Team addMemberToTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        // Add user to team
        user.setTeam(team);
        userRepository.save(user);
        
        return teamRepository.findById(teamId).orElseThrow();
    }
    
    /**
     * Remove a member from a team
     */
    @Transactional
    public Team removeMemberFromTeam(Long teamId, Long userId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        
        // Cannot remove the team manager this way
        if (user.equals(team.getManager())) {
            throw new IllegalStateException("Cannot remove the team manager. Assign a new manager first.");
        }
        
        // Remove user from team
        user.setTeam(null);
        userRepository.save(user);
        
        return teamRepository.findById(teamId).orElseThrow();
    }
    
    /**
     * Assign a manager to a team
     */
    @Transactional
    public Team assignManager(Long teamId, Long managerId) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        User manager = userRepository.findById(managerId).orElseThrow();
        
        // Ensure the user has manager role
        if (!"MANAGER".equals(manager.getRole())) {
            manager.setRole("MANAGER");
        }
        
        // Set manager relationship
        team.setManager(manager);
        manager.setTeam(team);
        
        userRepository.save(manager);
        return teamRepository.save(team);
    }
}