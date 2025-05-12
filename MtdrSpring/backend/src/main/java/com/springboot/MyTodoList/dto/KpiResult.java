package com.springboot.MyTodoList.dto;

import java.util.List;

public class KpiResult {
    private KpiData data;
    private List<SprintData> sprintData;
    private List<SprintDataForPie> sprintHours;
    private List<SprintDataForPie> sprintTasks;
    private List<SprintForTask> sprintsForTasks;

    public KpiResult() {
    }

    public KpiData getData() {
        return data;
    }

    public void setData(KpiData data) {
        this.data = data;
    }

    public List<SprintData> getSprintData() {
        return sprintData;
    }

    public void setSprintData(List<SprintData> sprintData) {
        this.sprintData = sprintData;
    }

    public List<SprintDataForPie> getSprintHours() {
        return sprintHours;
    }

    public void setSprintHours(List<SprintDataForPie> sprintHours) {
        this.sprintHours = sprintHours;
    }

    public List<SprintDataForPie> getSprintTasks() {
        return sprintTasks;
    }

    public void setSprintTasks(List<SprintDataForPie> sprintTasks) {
        this.sprintTasks = sprintTasks;
    }

    public List<SprintForTask> getSprintsForTasks() {
        return sprintsForTasks;
    }

    public void setSprintsForTasks(List<SprintForTask> sprintsForTasks) {
        this.sprintsForTasks = sprintsForTasks;
    }
}