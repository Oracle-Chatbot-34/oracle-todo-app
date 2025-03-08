package com.springboot.MyTodoList.model;

import java.time.OffsetDateTime;

import jakarta.persistence.*;

/*
    representation of the TODOITEM table that exists already
    in the autonomous database
 */
@Entity
@Table(name = "TODOITEM")
public class ToDoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int ID;

    @Column(name = "TITLE", nullable = false)
    String title;

    @Column(name = "DESCRIPTION")
    String description;

    @Column(name = "CREATION_TS")
    OffsetDateTime creation_ts;

    @Column(name = "DUE_DATE")
    OffsetDateTime dueDate;

    @Column(name = "ASSIGNEE_ID")
    private Long assigneeId;

    @Column(name = "TEAM_ID")
    private Long teamId;

    // Status values: SELECTED_FOR_DEVELOPMENT, IN_PROGRESS, DELAYED, IN_QA, DONE
    @Column(name = "STATUS")
    private String status;

    @Column(name = "ESTIMATED_HOURS")
    private Double estimatedHours;

    @Column(name = "ACTUAL_HOURS")
    private Double actualHours;

    @Column(name = "SPRINT_ID")
    private Long sprintId;

    @Column(name = "PRIORITY")
    private String priority;

    @Column(name = "DONE")
    boolean done;

    @Column(name = "COMPLETED_AT")
    private OffsetDateTime completedAt;

    public ToDoItem() {
    }

    public ToDoItem(int ID, String title, String description, OffsetDateTime creation_ts, OffsetDateTime dueDate,
            Long assigneeId, Long teamId, String status, String priority, boolean done, OffsetDateTime completedAt) {
        this.ID = ID;
        this.title = title;
        this.description = description;
        this.creation_ts = creation_ts;
        this.dueDate = dueDate;
        this.assigneeId = assigneeId;
        this.teamId = teamId;
        this.status = status;
        this.priority = priority;
        this.done = done;
        this.completedAt = completedAt;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public OffsetDateTime getCreation_ts() {
        return creation_ts;
    }

    public void setCreation_ts(OffsetDateTime creation_ts) {
        this.creation_ts = creation_ts;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;

        // Update done flag based on status
        this.done = "DONE".equals(status);

        // Update completion timestamp if done
        if (this.done && this.completedAt == null) {
            this.completedAt = OffsetDateTime.now();
        } else if (!this.done) {
            this.completedAt = null;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getPriority() {
        return priority;
    }

    public Double getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Double estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public String toString() {
        return "ToDoItem{" +
                "ID=" + ID +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", creation_ts=" + creation_ts +
                ", dueDate=" + dueDate +
                ", assigneeId=" + assigneeId +
                ", teamId=" + teamId +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", done=" + done +
                ", completedAt=" + completedAt +
                '}';
    }
}