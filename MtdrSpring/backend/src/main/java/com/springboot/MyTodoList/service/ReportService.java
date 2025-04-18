package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.KpiData;
import com.springboot.MyTodoList.dto.ReportRequest;
import com.springboot.MyTodoList.dto.SprintTasksReport;
import com.springboot.MyTodoList.dto.TaskInfo;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ToDoItemRepository todoItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KpiService kpiService;
    
    @Autowired
    private InsightService insightService;
    
    @Autowired
    private SprintRepository sprintRepository;
    
    /**
     * Generate a report based on the provided request parameters
     */
    public Map<String, Object> generateReport(ReportRequest request) {
        Map<String, Object> reportData = new HashMap<>();
        
        // Set default dates if not provided
        if (request.getStartDate() == null) {
            request.setStartDate(OffsetDateTime.now().minusDays(30));
        }
        
        if (request.getEndDate() == null) {
            request.setEndDate(OffsetDateTime.now());
        }
        
        // Report metadata
        reportData.put("generatedAt", OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        reportData.put("dateRange", Map.of(
            "start", request.getStartDate().format(DateTimeFormatter.ISO_DATE_TIME),
            "end", request.getEndDate().format(DateTimeFormatter.ISO_DATE_TIME)
        ));
        
        // Filter tasks based on request parameters
        List<ToDoItem> filteredTasks = getFilteredTasks(request);
        reportData.put("totalTasks", filteredTasks.size());
        
        // Add task status breakdown
        Map<String, Long> statusCounts = filteredTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getStatus() != null ? task.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
        reportData.put("statusBreakdown", statusCounts);
        
        // Set report type first - ensures it's always set regardless of path
        reportData.put("reportType", request.isIndividual() ? "Individual" : "Team");
        
        // Add KPI data
        KpiData kpiData;
        if (request.isIndividual() && request.getUserId() != null) {
            // Individual report
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                reportData.put("user", Map.of(
                    "id", user.getId(),
                    "name", user.getFullName(),
                    "role", user.getRole()
                ));
            }
            
            kpiData = kpiService.calculateUserKpis(
                    request.getUserId(), 
                    request.getStartDate(), 
                    request.getEndDate());
        } else {
            // Team report
            reportData.put("teamId", request.getTeamId());
            
            // Get team members
            List<User> teamMembers = userRepository.findByTeamId(request.getTeamId());
            reportData.put("teamSize", teamMembers.size());
            reportData.put("teamMembers", teamMembers.stream()
                    .map(user -> Map.of(
                        "id", user.getId(),
                        "name", user.getFullName(),
                        "role", user.getRole()
                    ))
                    .collect(Collectors.toList()));
            
            kpiData = kpiService.calculateTeamKpis(
                    request.getTeamId(), 
                    request.getStartDate(), 
                    request.getEndDate());
        }
        
        reportData.put("kpiData", kpiData);
        
        // Add task details
        reportData.put("tasks", filteredTasks.stream()
                .map(this::convertTaskToMap)
                .collect(Collectors.toList()));
        
        return reportData;
    }
    
    /**
     * Get tasks from the last sprint grouped by developer with AI insights
     */
    public SprintTasksReport getLastSprintTasksReport(Long sprintId, Long userId, Long teamId) {
        SprintTasksReport report = new SprintTasksReport();
        
        // Find the sprint (either specified or last completed)
        Sprint sprint;
        if (sprintId != null) {
            sprint = sprintRepository.findById(sprintId)
                    .orElseThrow(() -> new IllegalArgumentException("Sprint not found with ID: " + sprintId));
        } else {
            // Find the most recently completed sprint
            sprint = findLastCompletedSprint(teamId);
            if (sprint == null) {
                throw new IllegalArgumentException("No completed sprints found");
            }
        }
        
        // Set sprint details
        report.setSprintId(sprint.getId());
        report.setSprintName(sprint.getName());
        report.setStartDate(sprint.getStartDate());
        report.setEndDate(sprint.getEndDate());
        
        // Get tasks for this sprint
        List<ToDoItem> sprintTasks = todoItemRepository.findBySprintId(sprint.getId());
        
        // If userId is specified, filter for just that user
        if (userId != null) {
            sprintTasks = sprintTasks.stream()
                    .filter(task -> userId.equals(task.getAssigneeId()))
                    .collect(Collectors.toList());
        }
        
        // Group tasks by developer
        Map<Long, List<ToDoItem>> tasksByDeveloper = sprintTasks.stream()
                .filter(task -> task.getAssigneeId() != null)
                .collect(Collectors.groupingBy(ToDoItem::getAssigneeId));
        
        // Create developer groups
        List<SprintTasksReport.DeveloperTaskGroup> developerGroups = new ArrayList<>();
        for (Map.Entry<Long, List<ToDoItem>> entry : tasksByDeveloper.entrySet()) {
            Long developerId = entry.getKey();
            List<ToDoItem> developerTasks = entry.getValue();
            
            // Get developer details
            Optional<User> developerOpt = userRepository.findById(developerId);
            if (developerOpt.isPresent()) {
                User developer = developerOpt.get();
                
                SprintTasksReport.DeveloperTaskGroup group = new SprintTasksReport.DeveloperTaskGroup();
                group.setUserId(developer.getId());
                group.setFullName(developer.getFullName());
                group.setRole(developer.getRole());
                
                // Convert tasks to TaskInfo DTOs
                List<TaskInfo> taskInfos = developerTasks.stream()
                        .map(this::convertToTaskInfo)
                        .collect(Collectors.toList());
                group.setTasks(taskInfos);
                
                // Calculate statistics
                Map<String, Object> stats = calculateTaskStats(developerTasks);
                group.setStats(stats);
                
                // Generate AI insights
                String aiInsights = insightService.generateInsights(stats, true);
                group.setAiInsights(aiInsights);
                
                developerGroups.add(group);
            }
        }
        
        // Sort developer groups by name
        developerGroups.sort(Comparator.comparing(SprintTasksReport.DeveloperTaskGroup::getFullName));
        report.setDeveloperGroups(developerGroups);
        
        // Calculate team statistics
        Map<String, Object> teamStats = calculateTaskStats(sprintTasks);
        report.setTeamStats(teamStats);
        
        // Generate team insights
        String teamInsights = insightService.generateInsights(teamStats, false);
        report.setTeamInsights(teamInsights);
        
        return report;
    }
    
    /**
     * Filter tasks based on request parameters
     */
    private List<ToDoItem> getFilteredTasks(ReportRequest request) {
        List<ToDoItem> tasks;
        
        // Get tasks based on user or team
        if (request.isIndividual() && request.getUserId() != null) {
            tasks = todoItemRepository.findByAssigneeId(request.getUserId());
        } else {
            tasks = todoItemRepository.findByTeamId(request.getTeamId());
        }
        
        // Filter by date range
        List<ToDoItem> dateFilteredTasks = tasks.stream()
                .filter(task -> {
                    OffsetDateTime taskDate = task.getCreationTs();
                    return taskDate != null && 
                           (taskDate.isEqual(request.getStartDate()) || taskDate.isAfter(request.getStartDate())) && 
                           (taskDate.isEqual(request.getEndDate()) || taskDate.isBefore(request.getEndDate()));
                })
                .collect(Collectors.toList());
        
        // Filter by statuses if provided
        if (request.getStatuses() != null && !request.getStatuses().isEmpty()) {
            return dateFilteredTasks.stream()
                    .filter(task -> task.getStatus() != null && request.getStatuses().contains(task.getStatus()))
                    .collect(Collectors.toList());
        }
        
        return dateFilteredTasks;
    }
    
    /**
     * Helper method to convert ToDoItem to Map
     */
    private Map<String, Object> convertTaskToMap(ToDoItem task) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("id", task.getID());
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("status", task.getStatus());
        taskMap.put("priority", task.getPriority());
        taskMap.put("createdAt", task.getCreationTs());
        taskMap.put("dueDate", task.getDueDate());
        taskMap.put("completedAt", task.getCompletedAt());
        taskMap.put("estimatedHours", task.getEstimatedHours());
        taskMap.put("actualHours", task.getActualHours());
        
        // Get assignee name if available
        if (task.getAssigneeId() != null) {
            Optional<User> assigneeOpt = userRepository.findById(task.getAssigneeId());
            assigneeOpt.ifPresent(user -> 
                taskMap.put("assignee", Map.of(
                    "id", user.getId(),
                    "name", user.getFullName()
                ))
            );
        }
        
        return taskMap;
    }
    
    /**
     * Convert ToDoItem to TaskInfo DTO
     */
    private TaskInfo convertToTaskInfo(ToDoItem task) {
        TaskInfo info = new TaskInfo();
        info.setId((long) task.getID());
        info.setTitle(task.getTitle());
        info.setDescription(task.getDescription());
        info.setStatus(task.getStatus());
        info.setPriority(task.getPriority());
        info.setEstimatedHours(task.getEstimatedHours());
        info.setActualHours(task.getActualHours());
        info.setAssigneeId(task.getAssigneeId());
        info.setDueDate(task.getDueDate());
        info.setCompletedAt(task.getCompletedAt());
        
        // Set assignee name if available
        if (task.getAssigneeId() != null) {
            userRepository.findById(task.getAssigneeId())
                .ifPresent(user -> info.setAssigneeName(user.getFullName()));
        }
        
        return info;
    }
    
    /**
     * Calculate statistics for a list of tasks
     */
    private Map<String, Object> calculateTaskStats(List<ToDoItem> tasks) {
        Map<String, Object> stats = new HashMap<>();
        
        // Total tasks
        stats.put("totalTasks", tasks.size());
        
        // Completed tasks
        long completedTasks = tasks.stream()
                .filter(t -> "DONE".equals(t.getStatus()) || "COMPLETED".equals(t.getStatus()))
                .count();
        stats.put("completedTasks", completedTasks);
        
        // Completion rate
        double completionRate = tasks.isEmpty() ? 0 : (double) completedTasks / tasks.size() * 100;
        stats.put("completionRate", completionRate);
        
        // Hours metrics
        double totalEstimatedHours = tasks.stream()
                .filter(t -> t.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();
        stats.put("totalEstimatedHours", totalEstimatedHours);
        
        double totalActualHours = tasks.stream()
                .filter(t -> t.getActualHours() != null)
                .mapToDouble(ToDoItem::getActualHours)
                .sum();
        stats.put("totalActualHours", totalActualHours);
        
        // Time efficiency (actual vs estimated)
        double timeEfficiency = totalEstimatedHours > 0 ? (totalEstimatedHours / totalActualHours) * 100 : 0;
        stats.put("timeEfficiency", timeEfficiency);
        
        // On-time completion
        long onTimeCompletions = tasks.stream()
                .filter(t -> t.isDone() && t.getDueDate() != null && t.getCompletedAt() != null)
                .filter(t -> !t.getCompletedAt().isAfter(t.getDueDate()))
                .count();
        stats.put("onTimeCompletions", onTimeCompletions);
        
        double onTimeRate = completedTasks > 0 ? (double) onTimeCompletions / completedTasks * 100 : 0;
        stats.put("onTimeRate", onTimeRate);
        
        return stats;
    }
    
    /**
     * Find the last completed sprint
     */
    private Sprint findLastCompletedSprint(Long teamId) {
        List<Sprint> sprints;
        
        if (teamId != null) {
            // Get sprints for a specific team
            sprints = sprintRepository.findByTeamId(teamId);
        } else {
            // Get all sprints
            sprints = sprintRepository.findAll();
        }
        
        // Filter for completed sprints and sort by end date (descending)
        return sprints.stream()
                .filter(s -> "COMPLETED".equals(s.getStatus()))
                .sorted(Comparator.comparing(Sprint::getEndDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);
    }
}