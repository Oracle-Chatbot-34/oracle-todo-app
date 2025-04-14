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
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiGraphQLService {

    @Autowired
    private ToDoItemRepository todoItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SprintRepository sprintRepository;

    @Autowired
    private InsightService insightService;

    public KpiResult generateKpiResult(Long userId, Long teamId, Long startSprintId, Long endSprintId) {
        // Get sprint range
        Sprint startSprint = sprintRepository.findById(startSprintId)
                .orElseThrow(() -> new IllegalArgumentException("Start sprint not found"));

        Sprint endSprint = endSprintId != null
                ? sprintRepository.findById(endSprintId)
                        .orElseThrow(() -> new IllegalArgumentException("End sprint not found"))
                : startSprint;

        // Calculate date range based on sprints
        OffsetDateTime startDate = startSprint.getStartDate();
        OffsetDateTime endDate = endSprint.getEndDate() != null
                ? endSprint.getEndDate()
                : OffsetDateTime.now();

        // Get all sprints in range
        List<Sprint> sprintsInRange = sprintRepository.findAll().stream()
                .filter(sprint -> {
                    Long sprintId = sprint.getId();
                    return sprintId >= startSprintId && (endSprintId == null || sprintId <= endSprintId);
                })
                .collect(Collectors.toList());

        // Get users to analyze
        List<User> users = new ArrayList<>();
        boolean isIndividual = userId != null;

        if (isIndividual) {
            // Individual analysis
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            users.add(user);
        } else {
            // Team analysis
            users = userRepository.findByTeamId(teamId);
            if (users.isEmpty()) {
                throw new IllegalArgumentException("No users found for team");
            }
        }

        // Calculate basic KPI data
        KpiData kpiData = calculateKpiData(users, startDate, endDate, userId, teamId);

        // Generate chart data
        ChartData chartData = generateChartData(users, sprintsInRange);

        // Generate AI insights
        Map<String, Object> dataForInsights = new HashMap<>();
        dataForInsights.put("kpiData", kpiData);
        dataForInsights.put("sprintCount", sprintsInRange.size());
        dataForInsights.put("userCount", users.size());

        String insights = insightService.generateInsights(dataForInsights, isIndividual);

        // Combine everything into a result
        return new KpiResult(kpiData, chartData, insights);
    }

    private KpiData calculateKpiData(List<User> users, OffsetDateTime startDate, OffsetDateTime endDate,
            Long userId, Long teamId) {
        // Create new KPI data object
        KpiData kpiData = new KpiData();

        // Get all tasks for these users in date range
        List<ToDoItem> allTasks = new ArrayList<>();
        for (User user : users) {
            List<ToDoItem> userTasks = todoItemRepository.findByAssigneeId(user.getId()).stream()
                    .filter(task -> {
                        OffsetDateTime taskDate = task.getCreationTs();
                        return taskDate != null &&
                                (taskDate.isEqual(startDate) || taskDate.isAfter(startDate)) &&
                                (taskDate.isEqual(endDate) || taskDate.isBefore(endDate));
                    })
                    .collect(Collectors.toList());
            allTasks.addAll(userTasks);
        }

        if (allTasks.isEmpty()) {
            // Return empty data if no tasks found
            kpiData.setTaskCompletionRate(0.0);
            kpiData.setOnTimeCompletionRate(0.0);
            kpiData.setOverdueTasksRate(0.0);
            kpiData.setInProgressRate(0.0);
            kpiData.setWorkedHours(0.0);
            kpiData.setPlannedHours(0.0);
            kpiData.setHoursUtilizationPercent(0.0);
            kpiData.setTasksCompletedPerWeek(0.0);
            kpiData.setAverageTasksPerEmployee(0.0);
            kpiData.setStartDate(startDate);
            kpiData.setEndDate(endDate);
            kpiData.setUserId(userId);
            kpiData.setTeamId(teamId);
            return kpiData;
        }

        // Task completion rate
        long completedTasks = allTasks.stream()
                .filter(ToDoItem::isDone)
                .count();
        double taskCompletionRate = calculatePercentage(completedTasks, allTasks.size());
        kpiData.setTaskCompletionRate(taskCompletionRate);

        // Calculate task completion trend (weekly)
        Map<String, List<ToDoItem>> tasksByWeek = allTasks.stream()
                .collect(Collectors.groupingBy(task -> {
                    // Format as "YYYY-WW" (year and week number)
                    return task.getCreationTs().format(DateTimeFormatter.ofPattern("YYYY-w"));
                }));

        List<Double> weeklyTrends = new ArrayList<>();
        List<String> weekLabels = new ArrayList<>();

        // Sort weeks chronologically and calculate completion rate for each
        tasksByWeek.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    long weeklyCompleted = entry.getValue().stream()
                            .filter(ToDoItem::isDone)
                            .count();

                    double weeklyRate = calculatePercentage(weeklyCompleted, entry.getValue().size());
                    weeklyTrends.add(weeklyRate);

                    weekLabels.add(entry.getKey());
                });

        kpiData.setTaskCompletionTrend(weeklyTrends);
        kpiData.setTrendLabels(weekLabels);

        // Time completion rates
        long onTimeCompletions = 0;
        long overdueTasks = 0;
        long inProgressTasks = allTasks.stream()
                .filter(task -> !task.isDone() && "IN_PROGRESS".equals(task.getStatus()))
                .count();

        for (ToDoItem task : allTasks) {
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
        } else {
            kpiData.setOnTimeCompletionRate(0.0);
            kpiData.setOverdueTasksRate(0.0);
        }

        // In-progress rate from total tasks
        kpiData.setInProgressRate(calculatePercentage(inProgressTasks, allTasks.size()));

        // Hours calculations
        double totalEstimatedHours = allTasks.stream()
                .filter(task -> task.getEstimatedHours() != null)
                .mapToDouble(ToDoItem::getEstimatedHours)
                .sum();

        double totalActualHours = allTasks.stream()
                .filter(task -> task.getActualHours() != null)
                .mapToDouble(ToDoItem::getActualHours)
                .sum();

        kpiData.setPlannedHours(totalEstimatedHours);
        kpiData.setWorkedHours(totalActualHours);

        // Calculate hours utilization percentage
        if (totalEstimatedHours > 0) {
            kpiData.setHoursUtilizationPercent((totalActualHours / totalEstimatedHours) * 100);
        } else {
            kpiData.setHoursUtilizationPercent(0.0);
        }

        // Calculate tasks completed per week
        long weeks = 1 + ChronoUnit.WEEKS.between(startDate, endDate);
        if (weeks < 1)
            weeks = 1;

        kpiData.setTasksCompletedPerWeek((double) completedTasks / weeks);

        // Calculate average tasks per employee
        if (users.size() > 0) {
            kpiData.setAverageTasksPerEmployee((double) allTasks.size() / users.size());
        } else {
            kpiData.setAverageTasksPerEmployee(0.0);
        }

        // Resource utilization (simulated - would need real OCI metrics)
        kpiData.setOciResourcesUtilization(85.0); // Sample value

        // Set remaining metadata
        kpiData.setStartDate(startDate);
        kpiData.setEndDate(endDate);
        kpiData.setUserId(userId);
        kpiData.setTeamId(teamId);

        return kpiData;
    }

    private ChartData generateChartData(List<User> users, List<Sprint> sprints) {
        ChartData chartData = new ChartData();

        // Sort sprints by ID
        sprints.sort(Comparator.comparing(Sprint::getId));

        // Initialize data structures for charts
        List<DeveloperMetric> hoursByDeveloper = new ArrayList<>();
        List<DeveloperMetric> tasksByDeveloper = new ArrayList<>();
        List<SprintMetric> hoursBySprint = new ArrayList<>();
        List<SprintMetric> tasksBySprint = new ArrayList<>();
        List<SprintTaskInfo> taskInformation = new ArrayList<>();

        // Sprint names for reference in developer metrics
        List<String> sprintNames = sprints.stream()
                .map(Sprint::getName)
                .collect(Collectors.toList());

        // Calculate metrics for each developer
        for (User user : users) {
            Long userId = user.getId();
            String userName = user.getFullName();

            // Initialize developer metrics
            DeveloperMetric hoursMetric = new DeveloperMetric();
            hoursMetric.setDeveloperId(userId);
            hoursMetric.setDeveloperName(userName);
            hoursMetric.setSprints(sprintNames);
            hoursMetric.setValues(new ArrayList<>());

            DeveloperMetric tasksMetric = new DeveloperMetric();
            tasksMetric.setDeveloperId(userId);
            tasksMetric.setDeveloperName(userName);
            tasksMetric.setSprints(sprintNames);
            tasksMetric.setValues(new ArrayList<>());

            // For each sprint, calculate hours and tasks for this developer
            for (Sprint sprint : sprints) {
                Long sprintId = sprint.getId();

                // Get tasks for this user in this sprint
                List<ToDoItem> userSprintTasks = todoItemRepository.findByAssigneeIdAndSprintId(userId, sprintId);

                // Calculate total hours for user in sprint
                double sprintHours = userSprintTasks.stream()
                        .filter(task -> task.getActualHours() != null)
                        .mapToDouble(ToDoItem::getActualHours)
                        .sum();

                // Count completed tasks
                long completedTasks = userSprintTasks.stream()
                        .filter(ToDoItem::isDone)
                        .count();

                // Add values to metrics
                hoursMetric.getValues().add(sprintHours);
                tasksMetric.getValues().add((double) completedTasks);
            }

            // Add metrics to results
            hoursByDeveloper.add(hoursMetric);
            tasksByDeveloper.add(tasksMetric);
        }

        // Calculate metrics by sprint
        for (Sprint sprint : sprints) {
            Long sprintId = sprint.getId();
            String sprintName = sprint.getName();

            // Get all tasks in this sprint
            List<ToDoItem> sprintTasks = todoItemRepository.findBySprintId(sprintId);

            // Calculate total hours for sprint
            double sprintHours = sprintTasks.stream()
                    .filter(task -> task.getActualHours() != null)
                    .mapToDouble(ToDoItem::getActualHours)
                    .sum();

            // Count completed tasks
            long completedTasks = sprintTasks.stream()
                    .filter(ToDoItem::isDone)
                    .count();

            // Add metrics for hours
            SprintMetric hoursMetric = new SprintMetric();
            hoursMetric.setSprintId(sprintId);
            hoursMetric.setSprintName(sprintName);
            hoursMetric.setValue(sprintHours);
            hoursBySprint.add(hoursMetric);

            // Add metrics for tasks
            SprintMetric tasksMetric = new SprintMetric();
            tasksMetric.setSprintId(sprintId);
            tasksMetric.setSprintName(sprintName);
            tasksMetric.setValue((double) completedTasks);
            tasksBySprint.add(tasksMetric);

            // Create task information
            SprintTaskInfo taskInfo = new SprintTaskInfo();
            taskInfo.setSprintId(sprintId);
            taskInfo.setSprintName(sprintName);

            // Map tasks to task info objects
            List<TaskInfo> taskInfoList = sprintTasks.stream()
                    .map(this::mapToTaskInfo)
                    .collect(Collectors.toList());

            taskInfo.setTasks(taskInfoList);
            taskInformation.add(taskInfo);
        }

        // Set all chart data
        chartData.setHoursByDeveloper(hoursByDeveloper);
        chartData.setTasksByDeveloper(tasksByDeveloper);
        chartData.setHoursBySprint(hoursBySprint);
        chartData.setTasksBySprint(tasksBySprint);
        chartData.setTaskInformation(taskInformation);

        return chartData;
    }

    private TaskInfo mapToTaskInfo(ToDoItem task) {
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

        // Get assignee name if available
        if (task.getAssigneeId() != null) {
            User assignee = userRepository.findById(task.getAssigneeId()).orElse(null);
            if (assignee != null) {
                info.setAssigneeName(assignee.getFullName());
            }
        }

        return info;
    }

    private double calculatePercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return ((double) numerator / denominator) * 100;
    }
}