package com.springboot.MyTodoList.model.bot;

import com.springboot.MyTodoList.model.User;

/**
 * Class to track user state in the Telegram bot
 */
public class UserBotState {

    // User information
    private User user;
    private boolean authenticated = false;

    // Task creation
    private boolean newTaskMode = false;
    private String taskCreationStage;
    private String tempTaskTitle;
    private String tempTaskDescription;
    private Double tempEstimatedHours;
    private Long tempAssigneeId;
    private String tempPriority;

    // Task completion
    private boolean taskCompletionMode = false;
    private String taskCompletionStage;
    private int tempTaskId;
    private double tempActualHours;

    // Sprint mode
    private boolean sprintMode = false;
    private String sprintModeStage;

    // Legacy sprint states (for backward compatibility)
    private boolean sprintCreationMode = false;
    private String sprintCreationStage;
    private boolean endSprintMode = false;

    // Shared sprint fields
    private String tempSprintName;
    private String tempSprintDescription;
    private String tempSprintStartDate;
    private String tempSprintEndDate;
    private Long tempSprintId;

    // Task to sprint assignment
    private boolean assignToSprintMode = false;
    private String assignToSprintStage;

    // Getters and setters

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    // Task creation getters and setters

    public boolean isNewTaskMode() {
        return newTaskMode;
    }

    public void setNewTaskMode(boolean newTaskMode) {
        this.newTaskMode = newTaskMode;
    }

    public String getTaskCreationStage() {
        return taskCreationStage;
    }

    public void setTaskCreationStage(String taskCreationStage) {
        this.taskCreationStage = taskCreationStage;
    }

    public String getTempTaskTitle() {
        return tempTaskTitle;
    }

    public void setTempTaskTitle(String tempTaskTitle) {
        this.tempTaskTitle = tempTaskTitle;
    }

    public String getTempTaskDescription() {
        return tempTaskDescription;
    }

    public void setTempTaskDescription(String tempTaskDescription) {
        this.tempTaskDescription = tempTaskDescription;
    }

    public Double getTempEstimatedHours() {
        return tempEstimatedHours;
    }

    public void setTempEstimatedHours(Double tempEstimatedHours) {
        this.tempEstimatedHours = tempEstimatedHours;
    }

    public Long getTempAssigneeId() {
        return tempAssigneeId;
    }

    public void setTempAssigneeId(Long tempAssigneeId) {
        this.tempAssigneeId = tempAssigneeId;
    }

    public String getTempPriority() {
        return tempPriority;
    }

    public void setTempPriority(String tempPriority) {
        this.tempPriority = tempPriority;
    }

    // Task completion getters and setters

    public boolean isTaskCompletionMode() {
        return taskCompletionMode;
    }

    public void setTaskCompletionMode(boolean taskCompletionMode) {
        this.taskCompletionMode = taskCompletionMode;
    }

    public String getTaskCompletionStage() {
        return taskCompletionStage;
    }

    public void setTaskCompletionStage(String taskCompletionStage) {
        this.taskCompletionStage = taskCompletionStage;
    }

    public int getTempTaskId() {
        return tempTaskId;
    }

    public void setTempTaskId(int tempTaskId) {
        this.tempTaskId = tempTaskId;
    }

    public double getTempActualHours() {
        return tempActualHours;
    }

    public void setTempActualHours(double tempActualHours) {
        this.tempActualHours = tempActualHours;
    }

    // Sprint mode getters and setters

    public boolean isSprintMode() {
        return sprintMode;
    }

    public void setSprintMode(boolean sprintMode) {
        this.sprintMode = sprintMode;
    }

    public String getSprintModeStage() {
        return sprintModeStage;
    }

    public void setSprintModeStage(String sprintModeStage) {
        this.sprintModeStage = sprintModeStage;
    }

    // Legacy sprint getters and setters

    public boolean isSprintCreationMode() {
        return sprintCreationMode;
    }

    public void setSprintCreationMode(boolean sprintCreationMode) {
        this.sprintCreationMode = sprintCreationMode;
    }

    public String getSprintCreationStage() {
        return sprintCreationStage;
    }

    public void setSprintCreationStage(String sprintCreationStage) {
        this.sprintCreationStage = sprintCreationStage;
    }

    public boolean isEndSprintMode() {
        return endSprintMode;
    }

    public void setEndSprintMode(boolean endSprintMode) {
        this.endSprintMode = endSprintMode;
    }

    // Shared sprint fields getters and setters

    public String getTempSprintName() {
        return tempSprintName;
    }

    public void setTempSprintName(String tempSprintName) {
        this.tempSprintName = tempSprintName;
    }

    public String getTempSprintDescription() {
        return tempSprintDescription;
    }

    public void setTempSprintDescription(String tempSprintDescription) {
        this.tempSprintDescription = tempSprintDescription;
    }

    public String getTempSprintStartDate() {
        return tempSprintStartDate;
    }

    public void setTempSprintStartDate(String tempSprintStartDate) {
        this.tempSprintStartDate = tempSprintStartDate;
    }

    public String getTempSprintEndDate() {
        return tempSprintEndDate;
    }

    public void setTempSprintEndDate(String tempSprintEndDate) {
        this.tempSprintEndDate = tempSprintEndDate;
    }

    public Long getTempSprintId() {
        return tempSprintId;
    }

    public void setTempSprintId(Long tempSprintId) {
        this.tempSprintId = tempSprintId;
    }

    // Task to sprint assignment getters and setters

    public boolean isAssignToSprintMode() {
        return assignToSprintMode;
    }

    public void setAssignToSprintMode(boolean assignToSprintMode) {
        this.assignToSprintMode = assignToSprintMode;
    }

    public String getAssignToSprintStage() {
        return assignToSprintStage;
    }

    public void setAssignToSprintStage(String assignToSprintStage) {
        this.assignToSprintStage = assignToSprintStage;
    }

    // Reset methods

    public void resetTaskCreation() {
        this.newTaskMode = false;
        this.taskCreationStage = null;
        this.tempTaskTitle = null;
        this.tempTaskDescription = null;
        this.tempEstimatedHours = null;
        this.tempAssigneeId = null;
        this.tempPriority = null;
    }

    public void resetTaskCompletion() {
        this.taskCompletionMode = false;
        this.taskCompletionStage = null;
        this.tempTaskId = 0;
        this.tempActualHours = 0;
    }

    public void resetSprintCreation() {
        this.sprintCreationMode = false;
        this.sprintCreationStage = null;
        this.tempSprintName = null;
        this.tempSprintDescription = null;
        this.tempSprintStartDate = null;
        this.tempSprintEndDate = null;
    }

    public void resetAssignToSprint() {
        this.assignToSprintMode = false;
        this.assignToSprintStage = null;
        this.tempSprintId = null;
    }
}