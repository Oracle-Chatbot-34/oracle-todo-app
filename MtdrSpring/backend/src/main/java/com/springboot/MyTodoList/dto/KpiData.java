package com.springboot.MyTodoList.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class KpiData {

    // Task Completion Rate
    private double taskCompletionRate;
    private List<Double> taskCompletionTrend;
    private List<String> trendLabels;

    // Time Completion
    private double onTimeCompletionRate;
    private double overdueTasksRate;
    private double inProgressRate;

    // Resources Utilization
    private double ociResourcesUtilization;
    private double tasksCompletedPerWeek;

    // Real Hours Worked
    private double workedHours;
    private double plannedHours;
    private double hoursUtilizationPercent;

    // Average Tasks
    private double averageTasksPerEmployee;

    // Report date range
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;

    // User or team ID for which KPIs are calculated
    private Long userId;
    private Long teamId;

    // Getters and setters
    public double getTaskCompletionRate() {
        return taskCompletionRate;
    }

    public void setTaskCompletionRate(double taskCompletionRate) {
        this.taskCompletionRate = taskCompletionRate;
    }

    public List<Double> getTaskCompletionTrend() {
        return taskCompletionTrend;
    }

    public void setTaskCompletionTrend(List<Double> taskCompletionTrend) {
        this.taskCompletionTrend = taskCompletionTrend;
    }

    public List<String> getTrendLabels() {
        return trendLabels;
    }

    public void setTrendLabels(List<String> trendLabels) {
        this.trendLabels = trendLabels;
    }

    public double getOnTimeCompletionRate() {
        return onTimeCompletionRate;
    }

    public void setOnTimeCompletionRate(double onTimeCompletionRate) {
        this.onTimeCompletionRate = onTimeCompletionRate;
    }

    public double getOverdueTasksRate() {
        return overdueTasksRate;
    }

    public void setOverdueTasksRate(double overdueTasksRate) {
        this.overdueTasksRate = overdueTasksRate;
    }

    public double getInProgressRate() {
        return inProgressRate;
    }

    public void setInProgressRate(double inProgressRate) {
        this.inProgressRate = inProgressRate;
    }

    public double getOciResourcesUtilization() {
        return ociResourcesUtilization;
    }

    public void setOciResourcesUtilization(double ociResourcesUtilization) {
        this.ociResourcesUtilization = ociResourcesUtilization;
    }

    public double getTasksCompletedPerWeek() {
        return tasksCompletedPerWeek;
    }

    public void setTasksCompletedPerWeek(double tasksCompletedPerWeek) {
        this.tasksCompletedPerWeek = tasksCompletedPerWeek;
    }

    public double getWorkedHours() {
        return workedHours;
    }

    public void setWorkedHours(double workedHours) {
        this.workedHours = workedHours;
    }

    public double getPlannedHours() {
        return plannedHours;
    }

    public void setPlannedHours(double plannedHours) {
        this.plannedHours = plannedHours;
    }

    public double getHoursUtilizationPercent() {
        return hoursUtilizationPercent;
    }

    public void setHoursUtilizationPercent(double hoursUtilizationPercent) {
        this.hoursUtilizationPercent = hoursUtilizationPercent;
    }

    public double getAverageTasksPerEmployee() {
        return averageTasksPerEmployee;
    }

    public void setAverageTasksPerEmployee(double averageTasksPerEmployee) {
        this.averageTasksPerEmployee = averageTasksPerEmployee;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
}