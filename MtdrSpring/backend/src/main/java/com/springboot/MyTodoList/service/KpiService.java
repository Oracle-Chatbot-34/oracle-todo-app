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
        // Fetch user's tasks
        List<ToDoItem> userTasks = todoItemRepository.findByAssigneeId(userId);

        // Filter tasks by date range
        List<ToDoItem> tasksInRange = filterTasksByDateRange(userTasks, startDate, endDate);

        // Calculate KPIs
        KpiData kpiData = calculateKpis(tasksInRange);
        kpiData.setUserId(userId);
        kpiData.setStartDate(startDate);
        kpiData.setEndDate(endDate);

        return kpiData;
    }

    /**
     * Calculate KPIs for a team within a date range
     */
    public KpiData calculateTeamKpis(Long teamId, OffsetDateTime startDate, OffsetDateTime endDate) {
        // Fetch team's tasks
        List<ToDoItem> teamTasks = todoItemRepository.findByTeamId(teamId);

        // Filter tasks by date range
        List<ToDoItem> tasksInRange = filterTasksByDateRange(teamTasks, startDate, endDate);

        // Get team members count
        long teamMembersCount = userRepository.findByTeamId(teamId).size();

        // Calculate KPIs
        KpiData kpiData = calculateKpis(tasksInRange);
        kpiData.setTeamId(teamId);
        kpiData.setStartDate(startDate);
        kpiData.setEndDate(endDate);

        // Calculate average tasks per employee
        if (teamMembersCount > 0) {
            kpiData.setAverageTasksPerEmployee((double) tasksInRange.size() / teamMembersCount);
        }

        return kpiData;
    }

    /**
     * Filter tasks by date range
     */
    private List<ToDoItem> filterTasksByDateRange(List<ToDoItem> tasks, OffsetDateTime startDate,
            OffsetDateTime endDate) {
        return tasks.stream()
                .filter(task -> {
                    OffsetDateTime taskDate = task.getCreation_ts();
                    return taskDate != null &&
                            (taskDate.isEqual(startDate) || taskDate.isAfter(startDate)) &&
                            (taskDate.isEqual(endDate) || taskDate.isBefore(endDate));
                })
                .collect(Collectors.toList());
    }

    /**
     * Calculate KPIs from tasks
     */
    private KpiData calculateKpis(List<ToDoItem> tasks) {
        KpiData kpiData = new KpiData();

        if (tasks.isEmpty()) {
            return kpiData;
        }

        // Task Completion Rate
        long completedTasks = tasks.stream()
                .filter(ToDoItem::isDone)
                .count();

        kpiData.setTaskCompletionRate(calculatePercentage(completedTasks, tasks.size()));

        // Calculate task completion trend (weekly)
        Map<OffsetDateTime, List<ToDoItem>> tasksByWeek = tasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getCreation_ts().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))));

        List<Double> weeklyTrends = new ArrayList<>();
        List<String> weekLabels = new ArrayList<>();

        tasksByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    long weeklyCompleted = entry.getValue().stream()
                            .filter(ToDoItem::isDone)
                            .count();

                    double weeklyRate = calculatePercentage(weeklyCompleted, entry.getValue().size());
                    weeklyTrends.add(weeklyRate);

                    // Format week label
                    String weekLabel = entry.getKey().format(DateTimeFormatter.ofPattern("MMM W yyyy"));
                    weekLabels.add(weekLabel);
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

        // Real Hours Worked
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

        // OCI Resource Utilization (mock data - would need real OCI metrics)
        kpiData.setOciResourcesUtilization(85.0); // Sample value

        // Tasks completed per week average
        if (!weeklyTrends.isEmpty()) {
            double avgWeeklyCompletion = weeklyTrends.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            kpiData.setTasksCompletedPerWeek(avgWeeklyCompletion);
        }

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