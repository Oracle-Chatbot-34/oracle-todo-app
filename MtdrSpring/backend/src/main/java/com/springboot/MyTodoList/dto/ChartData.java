package com.springboot.MyTodoList.dto;

import java.util.List;

public class ChartData {
    private List<DeveloperMetric> hoursByDeveloper;
    private List<DeveloperMetric> tasksByDeveloper;
    private List<SprintMetric> hoursBySprint;
    private List<SprintMetric> tasksBySprint;
    private List<SprintTaskInfo> taskInformation;

    public List<DeveloperMetric> getHoursByDeveloper() {
        return hoursByDeveloper;
    }

    public void setHoursByDeveloper(List<DeveloperMetric> hoursByDeveloper) {
        this.hoursByDeveloper = hoursByDeveloper;
    }

    public List<DeveloperMetric> getTasksByDeveloper() {
        return tasksByDeveloper;
    }

    public void setTasksByDeveloper(List<DeveloperMetric> tasksByDeveloper) {
        this.tasksByDeveloper = tasksByDeveloper;
    }

    public List<SprintMetric> getHoursBySprint() {
        return hoursBySprint;
    }

    public void setHoursBySprint(List<SprintMetric> hoursBySprint) {
        this.hoursBySprint = hoursBySprint;
    }

    public List<SprintMetric> getTasksBySprint() {
        return tasksBySprint;
    }

    public void setTasksBySprint(List<SprintMetric> tasksBySprint) {
        this.tasksBySprint = tasksBySprint;
    }

    public List<SprintTaskInfo> getTaskInformation() {
        return taskInformation;
    }

    public void setTaskInformation(List<SprintTaskInfo> taskInformation) {
        this.taskInformation = taskInformation;
    }
}