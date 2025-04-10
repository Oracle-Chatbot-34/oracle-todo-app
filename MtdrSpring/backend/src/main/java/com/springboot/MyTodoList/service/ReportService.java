package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.KpiData;
import com.springboot.MyTodoList.dto.ReportRequest;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private ToDoItemRepository todoItemRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private KpiService kpiService;
    
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
        
        // Add KPI data
        KpiData kpiData;
        if (request.isIndividual() && request.getUserId() != null) {
            // Individual report
            Optional<User> userOpt = userRepository.findById(request.getUserId());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                reportData.put("reportType", "Individual");
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
            reportData.put("reportType", "Team");
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
                .map(task -> {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("id", task.getID());
                    taskMap.put("title", task.getTitle());
                    taskMap.put("description", task.getDescription());
                    taskMap.put("status", task.getStatus());
                    taskMap.put("priority", task.getPriority());
                    taskMap.put("createdAt", task.getCreation_ts());
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
                })
                .collect(Collectors.toList()));
        
        return reportData;
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
                    OffsetDateTime taskDate = task.getCreation_ts();
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
}