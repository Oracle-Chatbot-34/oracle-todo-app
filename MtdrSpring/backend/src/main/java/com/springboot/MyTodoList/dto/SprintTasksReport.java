package com.springboot.MyTodoList.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class SprintTasksReport {
    private Long sprintId;
    private String sprintName;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private List<DeveloperTaskGroup> developerGroups;
    private Map<String, Object> teamStats;
    private String teamInsights;
    
    public static class DeveloperTaskGroup {
        private Long userId;
        private String fullName;
        private String role;
        private List<TaskInfo> tasks;
        private Map<String, Object> stats;
        private String aiInsights;
        
        // Getters and setters
        public Long getUserId() {
            return userId;
        }
        
        public void setUserId(Long userId) {
            this.userId = userId;
        }
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public List<TaskInfo> getTasks() {
            return tasks;
        }
        
        public void setTasks(List<TaskInfo> tasks) {
            this.tasks = tasks;
        }
        
        public Map<String, Object> getStats() {
            return stats;
        }
        
        public void setStats(Map<String, Object> stats) {
            this.stats = stats;
        }
        
        public String getAiInsights() {
            return aiInsights;
        }
        
        public void setAiInsights(String aiInsights) {
            this.aiInsights = aiInsights;
        }
    }
    
    // Getters and setters
    public Long getSprintId() {
        return sprintId;
    }
    
    public void setSprintId(Long sprintId) {
        this.sprintId = sprintId;
    }
    
    public String getSprintName() {
        return sprintName;
    }
    
    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }
    
    public OffsetDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }
    
    public OffsetDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }
    
    public List<DeveloperTaskGroup> getDeveloperGroups() {
        return developerGroups;
    }
    
    public void setDeveloperGroups(List<DeveloperTaskGroup> developerGroups) {
        this.developerGroups = developerGroups;
    }
    
    public Map<String, Object> getTeamStats() {
        return teamStats;
    }
    
    public void setTeamStats(Map<String, Object> teamStats) {
        this.teamStats = teamStats;
    }
    
    public String getTeamInsights() {
        return teamInsights;
    }
    
    public void setTeamInsights(String teamInsights) {
        this.teamInsights = teamInsights;
    }
}