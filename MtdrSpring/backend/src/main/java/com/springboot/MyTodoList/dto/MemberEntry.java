package com.springboot.MyTodoList.dto;

public class MemberEntry {
    private String member;
    private Integer hours;
    private Integer tasksCompleted;

    public MemberEntry() {
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public Integer getHours() {
        return hours;
    }

    public void setHours(Integer hours) {
        this.hours = hours;
    }

    public Integer getTasksCompleted() {
        return tasksCompleted;
    }

    public void setTasksCompleted(Integer tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }
}