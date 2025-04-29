package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.*;
import com.springboot.MyTodoList.model.Sprint;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.model.User;
import com.springboot.MyTodoList.repository.SprintRepository;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiGraphQLService {

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private ToDoItemRepository toDoItemRepository;

    @Autowired
    private UserRepository userRepository;

    public KpiResult generateKpiResult(Long startSprintId, Long endSprintId) {
        // Get start sprint (required)
        Sprint startSprint = sprintRepository.findById(startSprintId)
                .orElseThrow(() -> new IllegalArgumentException("Start sprint not found"));

        // Get end sprint (optional)
        Sprint endSprint = null;
        if (endSprintId != null) {
            endSprint = sprintRepository.findById(endSprintId)
                    .orElseThrow(() -> new IllegalArgumentException("End sprint not found"));
        }

        // Get all sprints in the range (including start and end)
        List<Sprint> sprintsInRange = getSprintsInRange(startSprint, endSprint);

        // Collect all ToDoItems for these sprints
        List<ToDoItem> allTasks = new ArrayList<>();
        for (Sprint sprint : sprintsInRange) {
            List<ToDoItem> tasksForSprint = toDoItemRepository.findBySprintId(sprint.getId());
            allTasks.addAll(tasksForSprint);
        }

        // Create the KPI result
        KpiResult result = new KpiResult();

        // 1. Set the core KPI data
        result.setData(calculateKpiData(sprintsInRange, allTasks));

        // 2. Generate sprint data with member entries
        result.setSprintData(generateSprintData(sprintsInRange));

        // 3. Generate sprint hours for pie chart
        result.setSprintHours(generateSprintHours(sprintsInRange));

        // 4. Generate sprint tasks for pie chart
        result.setSprintTasks(generateSprintTasks(sprintsInRange));

        // 5. Generate sprint references for task information
        result.setSprintsForTasks(generateSprintsForTasks(sprintsInRange));

        return result;
    }

    private List<Sprint> getSprintsInRange(Sprint startSprint, Sprint endSprint) {
        if (endSprint == null) {
            // If no end sprint is provided, just return the start sprint
            return Collections.singletonList(startSprint);
        }

        // Ensure startSprint.id <= endSprint.id
        if (startSprint.getId() > endSprint.getId()) {
            throw new IllegalArgumentException("Start sprint ID must be less than or equal to end sprint ID");
        }

        // Get all sprints with IDs between startSprint.id and endSprint.id (inclusive)
        return sprintRepository.findByIdBetweenOrderById(startSprint.getId(), endSprint.getId());
    }

    private KpiData calculateKpiData(List<Sprint> sprints, List<ToDoItem> tasks) {
        KpiData kpiData = new KpiData();

        // Basic statistics calculation
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream().filter(task -> "DONE".equals(task.getStatus())).count();
        int inProgressTasks = (int) tasks.stream().filter(task -> "IN_PROGRESS".equals(task.getStatus())).count();

        double totalEstimatedHours = tasks.stream()
                .filter(task -> task.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();

        double totalActualHours = tasks.stream()
                .filter(task -> task.getActualHours() != null)
                .mapToDouble(ToDoItem::getActualHours)
                .sum();

        // Calculate rates
        double taskCompletionRate = totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0;
        double inProgressRate = totalTasks > 0 ? (double) inProgressTasks / totalTasks * 100 : 0;
        double hoursUtilizationPercent = totalEstimatedHours > 0 ? totalActualHours / totalEstimatedHours * 100 : 0;

        // Get dates
        OffsetDateTime startDate = sprints.get(0).getStartDate();
        OffsetDateTime endDate = sprints.get(sprints.size() - 1).getEndDate();

        // Set the calculated data
        kpiData.setTaskCompletionRate(taskCompletionRate);
        kpiData.setInProgressRate(inProgressRate);
        kpiData.setWorkedHours(totalActualHours);
        kpiData.setPlannedHours(totalEstimatedHours);
        kpiData.setHoursUtilizationPercent(hoursUtilizationPercent);
        kpiData.setStartDate(startDate);
        kpiData.setEndDate(endDate);

        // Set team and user IDs (if available from context)
        // For demo purposes, we'll set null or get from the first sprint if available
        if (!sprints.isEmpty() && sprints.get(0).getTeam() != null) {
            kpiData.setTeamId(sprints.get(0).getTeam().getId());
        }

        return kpiData;
    }

    private List<SprintData> generateSprintData(List<Sprint> sprints) {
        List<SprintData> result = new ArrayList<>();

        for (Sprint sprint : sprints) {
            SprintData sprintData = new SprintData();
            sprintData.setId(sprint.getId().intValue());
            sprintData.setName(sprint.getName());

            // Get tasks for this sprint
            List<ToDoItem> sprintTasks = toDoItemRepository.findBySprintId(sprint.getId());

            // Group tasks by assignee and calculate hours and completed tasks
            Map<Long, MemberStats> memberStatsMap = new HashMap<>();

            for (ToDoItem task : sprintTasks) {
                Long assigneeId = task.getAssigneeId();
                if (assigneeId == null)
                    continue;

                MemberStats stats = memberStatsMap.computeIfAbsent(assigneeId, k -> new MemberStats());

                // Add hours if task has actual hours
                if (task.getActualHours() != null) {
                    stats.hours += task.getActualHours();
                }

                // Increment completed tasks if task is done
                if ("DONE".equals(task.getStatus())) {
                    stats.tasksCompleted++;
                }
            }

            // Create member entries
            List<MemberEntry> entries = new ArrayList<>();
            int totalHours = 0;
            int totalCompletedTasks = 0;

            for (Map.Entry<Long, MemberStats> entry : memberStatsMap.entrySet()) {
                Long userId = entry.getKey();
                MemberStats stats = entry.getValue();

                // Get user name
                String memberName = "Unknown";
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    memberName = userOpt.get().getFullName();
                }

                MemberEntry memberEntry = new MemberEntry();
                memberEntry.setMember(memberName);
                memberEntry.setHours(stats.hours.intValue());
                memberEntry.setTasksCompleted(stats.tasksCompleted);

                entries.add(memberEntry);

                totalHours += stats.hours.intValue();
                totalCompletedTasks += stats.tasksCompleted;
            }

            sprintData.setEntries(entries);
            sprintData.setTotalHours(totalHours);
            sprintData.setTotalTasks(totalCompletedTasks);

            result.add(sprintData);
        }

        return result;
    }

    private List<SprintDataForPie> generateSprintHours(List<Sprint> sprints) {
        List<SprintDataForPie> result = new ArrayList<>();

        for (Sprint sprint : sprints) {
            SprintDataForPie pieData = new SprintDataForPie();
            pieData.setId(sprint.getId().intValue());
            pieData.setName(sprint.getName());

            // Get tasks for this sprint
            List<ToDoItem> sprintTasks = toDoItemRepository.findBySprintId(sprint.getId());

            // Calculate total hours
            double totalHours = sprintTasks.stream()
                    .filter(task -> task.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();

            pieData.setCount((int) totalHours);
            result.add(pieData);
        }

        return result;
    }

    private List<SprintDataForPie> generateSprintTasks(List<Sprint> sprints) {
        List<SprintDataForPie> result = new ArrayList<>();

        for (Sprint sprint : sprints) {
            SprintDataForPie pieData = new SprintDataForPie();
            pieData.setId(sprint.getId().intValue());
            pieData.setName(sprint.getName());

            // Get tasks for this sprint
            List<ToDoItem> sprintTasks = toDoItemRepository.findBySprintId(sprint.getId());

            // Count completed tasks
            int completedTasks = (int) sprintTasks.stream()
                    .filter(task -> "DONE".equals(task.getStatus()))
                    .count();

            pieData.setCount(completedTasks);
            result.add(pieData);
        }

        return result;
    }

    private List<SprintForTask> generateSprintsForTasks(List<Sprint> sprints) {
        return sprints.stream()
                .map(sprint -> {
                    SprintForTask sprintRef = new SprintForTask();
                    sprintRef.setSprintId(sprint.getId().intValue());
                    sprintRef.setSprintName(sprint.getName());
                    return sprintRef;
                })
                .collect(Collectors.toList());
    }

    // Helper class to track member statistics
    private static class MemberStats {
        Double hours = 0.0;
        Integer tasksCompleted = 0;
    }
}