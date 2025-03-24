package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.model.Team;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.service.TeamService;
import com.springboot.MyTodoList.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private UserService userService;

    /**
     * Get all teams with simplified response format
     */
    @GetMapping("/teams")
    public ResponseEntity<List<Map<String, Object>>> getAllTeams() {
        List<Team> teams = teamService.findAll();

        // Convert to simplified team response
        List<Map<String, Object>> simplifiedTeams = teams.stream()
                .map(this::convertTeamToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(simplifiedTeams);
    }

    /**
     * Get team by ID with simplified response format
     */
    @GetMapping("/teams/{id}")
    public ResponseEntity<?> getTeamById(@PathVariable("id") Long id) {
        Optional<Team> teamOpt = teamService.findById(id);
        if (teamOpt.isPresent()) {
            Map<String, Object> teamData = convertTeamToMap(teamOpt.get());
            return ResponseEntity.ok(teamData);
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
            Map<String, Object> teamData = convertTeamToMap(createdTeam);
            return ResponseEntity.status(HttpStatus.CREATED).body(teamData);
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
            Map<String, Object> teamData = convertTeamToMap(updatedTeam);
            return ResponseEntity.ok(teamData);
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

            // Convert to simplified response format
            List<Map<String, Object>> simplifiedMembers = members.stream()
                    .map(this::convertUserToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(simplifiedMembers);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
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
            Map<String, Object> teamData = convertTeamToMap(team);
            return ResponseEntity.ok(teamData);
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
            Map<String, Object> teamData = convertTeamToMap(team);
            return ResponseEntity.ok(teamData);
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
            Map<String, Object> teamData = convertTeamToMap(team);
            return ResponseEntity.ok(teamData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    /**
     * Helper method to convert Team to Map with simplified representation
     */
    private Map<String, Object> convertTeamToMap(Team team) {
        Map<String, Object> teamData = new HashMap<>();
        teamData.put("id", team.getId());
        teamData.put("name", team.getName());
        teamData.put("description", team.getDescription());
        teamData.put("createdAt", team.getCreatedAt());

        // Add manager details if present
        if (team.getManager() != null) {
            Map<String, Object> managerData = new HashMap<>();
            managerData.put("id", team.getManager().getId());
            managerData.put("username", team.getManager().getUsername());
            managerData.put("fullName", team.getManager().getFullName());
            teamData.put("manager", managerData);
        }

        // Add member IDs only
        if (team.getMembers() != null) {
            List<Long> memberIds = team.getMembers().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
            teamData.put("memberIds", memberIds);
        }

        return teamData;
    }

    /**
     * Helper method to convert User to Map with simplified representation
     */
    private Map<String, Object> convertUserToMap(User member) {
        Map<String, Object> memberData = new HashMap<>();
        memberData.put("id", member.getId());
        memberData.put("username", member.getUsername());
        memberData.put("fullName", member.getFullName());
        memberData.put("role", member.getRole());
        memberData.put("employeeId", member.getEmployeeId());
        memberData.put("telegramId", member.getTelegramId());
        memberData.put("teamId", member.getTeam() != null ? member.getTeam().getId() : null);
        memberData.put("createdAt", member.getCreatedAt());
        memberData.put("updatedAt", member.getUpdatedAt());
        return memberData;
    }
}