package com.springboot.MyTodoList.service;

import com.springboot.MyTodoList.dto.KpiData;
import com.springboot.MyTodoList.model.ToDoItem;
import com.springboot.MyTodoList.repository.ToDoItemRepository;
import com.springboot.MyTodoList.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class KpiService {

    @Autowired
    private ToDoItemRepository todoItemRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Calculate KPIs for a user within a date range
     */
    public KpiData calculateUserKpis(Long userId, OffsetDateTime startDate, OffsetDateTime endDate) {
        // Use defaults if dates not provided
        OffsetDateTime effectiveStartDate = startDate != null ? startDate : OffsetDateTime.now().minusDays(30);
        OffsetDateTime effectiveEndDate = endDate != null ? endDate : OffsetDateTime.now();
        
        // Fetch user's tasks directly with date range to reduce memory usage
        List<ToDoItem> tasksInRange = todoItemRepository.findByAssigneeIdAndCreationTsBetween(
                userId, effectiveStartDate, effectiveEndDate);

        // Calculate KPIs
        KpiData kpiData = calculateKpis(tasksInRange);
        kpiData.setUserId(userId);
        kpiData.setStartDate(effectiveStartDate);
        kpiData.setEndDate(effectiveEndDate);

        return kpiData;
    }

    /**
     * Calculate KPIs for a team within a date range
     */
    public KpiData calculateTeamKpis(Long teamId, OffsetDateTime startDate, OffsetDateTime endDate) {
        // Use defaults if dates not provided
        OffsetDateTime effectiveStartDate = startDate != null ? startDate : OffsetDateTime.now().minusDays(30);
        OffsetDateTime effectiveEndDate = endDate != null ? endDate : OffsetDateTime.now();
        
        // Fetch team's tasks directly with date range
        List<ToDoItem> tasksInRange = todoItemRepository.findByTeamIdAndCreationTsBetween(
                teamId, effectiveStartDate, effectiveEndDate);

        // Get team members count for average calculations
        long teamMembersCount = userRepository.findByTeamId(teamId).size();

        // Calculate KPIs
        KpiData kpiData = calculateKpis(tasksInRange);
        kpiData.setTeamId(teamId);
        kpiData.setStartDate(effectiveStartDate);
        kpiData.setEndDate(effectiveEndDate);

        // Calculate average tasks per employee if team has members
        if (teamMembersCount > 0) {
            kpiData.setAverageTasksPerEmployee((double) tasksInRange.size() / teamMembersCount);
        }

        return kpiData;
    }

    /**
     * Calculate KPIs from tasks
     */
    private KpiData calculateKpis(List<ToDoItem> tasks) {
        KpiData kpiData = new KpiData();

        if (tasks.isEmpty()) {
            return kpiData;
        }

        // Current date for calculations
        OffsetDateTime now = OffsetDateTime.now();
        
        // Task Completion Rate
        long completedTasks = tasks.stream()
                .filter(ToDoItem::isDone)
                .count();

        kpiData.setTaskCompletionRate(calculatePercentage(completedTasks, tasks.size()));

        // Calculate task completion trend (weekly)
        Map<OffsetDateTime, List<ToDoItem>> tasksByWeek = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getCreationTs().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

        List<Double> weeklyTrends = new ArrayList<>();
        List<String> weekLabels = new ArrayList<>();
        
        DateTimeFormatter weekFormatter = DateTimeFormatter.ofPattern("MMM W yyyy");

        tasksByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    long weeklyCompleted = entry.getValue().stream()
                            .filter(ToDoItem::isDone)
                            .count();

                    double weeklyRate = calculatePercentage(weeklyCompleted, entry.getValue().size());
                    weeklyTrends.add(weeklyRate);
                    weekLabels.add(entry.getKey().format(weekFormatter));
                });

        kpiData.setTaskCompletionTrend(weeklyTrends);
        kpiData.setTrendLabels(weekLabels);

        // Time Completion Rates
        long onTimeCompletions = 0;
        long overdueTasks = 0;
        long inProgressTasks = tasks.stream()
                .filter(task -> !task.isDone() && "IN_PROGRESS".equals(task.getStatus()))
                .count();

        for (ToDoItem task : tasks) {
            if (task.isDone() && task.getDueDate() != null && task.getCompletedAt() != null) {
                if (task.getCompletedAt().isBefore(task.getDueDate()) ||
                        task.getCompletedAt().isEqual(task.getDueDate())) {
                    onTimeCompletions++;
                } else {
                    overdueTasks++;
                }
            }
        }

        // Only calculate rates if there are completed tasks
        if (completedTasks > 0) {
            kpiData.setOnTimeCompletionRate(calculatePercentage(onTimeCompletions, completedTasks));
            kpiData.setOverdueTasksRate(calculatePercentage(overdueTasks, completedTasks));
        }

        // In-progress rate from total tasks
        kpiData.setInProgressRate(calculatePercentage(inProgressTasks, tasks.size()));

        // Hours metrics
        double totalEstimatedHours = tasks.stream()
                .filter(task -> task.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();

        double totalActualHours = tasks.stream()
                .filter(task -> task.getActualHours() != null)
                .mapToDouble(ToDoItem::getActualHours)
                .sum();

        kpiData.setPlannedHours(totalEstimatedHours);
        kpiData.setWorkedHours(totalActualHours);

        // Calculate hours utilization percentage
        if (totalEstimatedHours > 0) {
            kpiData.setHoursUtilizationPercent((totalActualHours / totalEstimatedHours) * 100);
        }

        // Tasks completed per week average
        if (!weeklyTrends.isEmpty()) {
            // Calculate the number of weeks in the period
            long daysInPeriod = ChronoUnit.DAYS.between(
                    tasks.stream().map(ToDoItem::getCreationTs).min(OffsetDateTime::compareTo).orElse(now),
                    tasks.stream().map(ToDoItem::getCreationTs).max(OffsetDateTime::compareTo).orElse(now)
            );
            
            double weeksInPeriod = Math.max(1, daysInPeriod / 7.0);
            
            // Calculate tasks completed per week
            double tasksPerWeek = completedTasks / weeksInPeriod;
            kpiData.setTasksCompletedPerWeek(tasksPerWeek);
        }

        // Set resource utilization (placeholder value)
        kpiData.setOciResourcesUtilization(85.0);

        return kpiData;
    }

    /**
     * Helper method to calculate percentage
     */
    private double calculatePercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return ((double) numerator / denominator) * 100;
    }
}