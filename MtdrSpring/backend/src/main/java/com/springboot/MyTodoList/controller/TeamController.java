package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.TeamService;
import com.springboot.MyTodoList.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    /**
     * Get all teams
     */
    @GetMapping("/teams")
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamService.findAll();
        return ResponseEntity.ok(teams);
    }

    /**
     * Get team by ID
     */
    @GetMapping("/teams/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable("id") Long id) {
        Optional<Team> teamOpt = teamService.findById(id);
        if (teamOpt.isPresent()) {
            return ResponseEntity.ok(teamOpt.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with ID: " + id);
        }
    }

    /**
     * Create a new team
     */
    @PostMapping("/teams")
    public ResponseEntity<?> createTeam(@RequestBody Team team) {
        try {
            if (team.getManager() != null && team.getManager().getId() != null) {
                // If manager is specified by ID, fetch the user
                Optional<User> managerOpt = userService.findById(team.getManager().getId());
                if (managerOpt.isPresent()) {
                    team.setManager(managerOpt.get());
                }
            }

            Team createdTeam = teamService.createTeam(team);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Update a team
     */
    @PutMapping("/teams/{id}")
    public ResponseEntity<?> updateTeam(@PathVariable("id") Long id, @RequestBody Team team) {
        try {
            if (!id.equals(team.getId())) {
                return ResponseEntity.badRequest().body("Team ID in path does not match ID in body");
            }

            if (!teamService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with ID: " + id);
            }

            if (team.getManager() != null && team.getManager().getId() != null) {
                // If manager is specified by ID, fetch the user
                Optional<User> managerOpt = userService.findById(team.getManager().getId());
                if (managerOpt.isPresent()) {
                    team.setManager(managerOpt.get());
                }
            }

            Team updatedTeam = teamService.updateTeam(team);
            return ResponseEntity.ok(updatedTeam);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Delete a team
     */
    @DeleteMapping("/teams/{id}")
    public ResponseEntity<?> deleteTeam(@PathVariable("id") Long id) {
        try {
            if (!teamService.findById(id).isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with ID: " + id);
            }

            teamService.deleteTeam(id);
            return ResponseEntity.ok("Team deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Get team members
     */
    @GetMapping("/teams/{id}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable("id") Long id) {
        try {
            Optional<Team> teamOpt = teamService.findById(id);
            if (!teamOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Team not found with ID: " + id);
            }

            List<User> members = userService.findByTeamId(id);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Add member to team
     */
    @PostMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> addMemberToTeam(
            @PathVariable("teamId") Long teamId,
            @PathVariable("userId") Long userId) {
        try {
            Team team = teamService.addMemberToTeam(teamId, userId);
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Remove member from team
     */
    @DeleteMapping("/teams/{teamId}/members/{userId}")
    public ResponseEntity<?> removeMemberFromTeam(
            @PathVariable("teamId") Long teamId,
            @PathVariable("userId") Long userId) {
        try {
            Team team = teamService.removeMemberFromTeam(teamId, userId);
            return ResponseEntity.ok(team);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Assign manager to team
     */
    @PostMapping("/teams/{teamId}/manager/{userId}")
    public ResponseEntity<?> assignManager(
            @PathVariable("teamId") Long teamId,
            @PathVariable("userId") Long userId) {
        try {
            Team team = teamService.assignManager(teamId, userId);
            return ResponseEntity.ok(team);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}