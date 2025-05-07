package com.springboot.MyTodoList.dto;

import java.util.List;

public class SprintData {
    private Integer id;
    private String name;
    private List<MemberEntry> entries;
    private Integer totalHours;
    private Integer totalTasks;

    public SprintData() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MemberEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<MemberEntry> entries) {
        this.entries = entries;
    }

    public Integer getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(Integer totalHours) {
        this.totalHours = totalHours;
    }

    public Integer getTotalTasks() {
        return totalTasks;
    }

    public void setTotalTasks(Integer totalTasks) {
        this.totalTasks = totalTasks;
    }
}