package com.springboot.MyTodoList.dto;

public class SprintForTask {
    private Integer sprintId;
    private String sprintName;

    public SprintForTask() {
    }

    public Integer getSprintId() {
        return sprintId;
    }

    public void setSprintId(Integer sprintId) {
        this.sprintId = sprintId;
    }

    public String getSprintName() {
        return sprintName;
    }

    public void setSprintName(String sprintName) {
        this.sprintName = sprintName;
    }
}