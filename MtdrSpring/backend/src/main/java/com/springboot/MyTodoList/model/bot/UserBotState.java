package com.springboot.MyTodoList.model.bot;

import com.springboot.MyTodoList.model.User;

/**
 * Represents the state of a user's interaction with the Telegram bot.
 */
public class UserBotState {

    // Authentication state
    private boolean authenticated = false;
    private String employeeId = null;
    private User user = null;

    // Task creation state
    private boolean newTaskMode = false;
    private String taskCreationStage = null;
    private String tempTaskTitle = null;
    private String tempTaskDescription = null;
    private Double tempEstimatedHours = null;
    private String tempPriority = null;
    private Long tempAssigneeId = null;

    // Sprint creation state
    private boolean sprintCreationMode = false;
    private String sprintCreationStage = null;
    private String tempSprintName = null;
    private String tempSprintDescription = null;
    private String tempSprintStartDate = null;
    private String tempSprintEndDate = null;

    // End sprint state
    private boolean endSprintMode = false;

    // Task completion state
    private boolean taskCompletionMode = false;
    private String taskCompletionStage = null;
    private int tempTaskId = 0;
    private Double tempActualHours = null;

    // Assign to sprint state
    private boolean assignToSprintMode = false;
    private String assignToSprintStage = null;
    private Long tempSprintId = null;

    // Start task work state
    private boolean startTaskWorkMode = false;
    private String startTaskWorkStage = null;

    // General state
    private boolean viewingTaskMode = false;
    private Long currentTaskId = null;

    /**
     * Resets the user's state to unauthenticated with no active operations.
     */
    public void reset() {
        this.authenticated = false;
        this.employeeId = null;
        this.user = null;
        resetTaskCreation();
        resetTaskCompletion();
        resetAssignToSprint();
        resetStartTaskWork();
        resetSprintCreation();
        this.endSprintMode = false;
        this.viewingTaskMode = false;
        this.currentTaskId = null;
    }

    /**
     * Reset task creation state
     */
    public void resetTaskCreation() {
        this.newTaskMode = false;
        this.taskCreationStage = null;
        this.tempTaskTitle = null;
        this.tempTaskDescription = null;
        this.tempEstimatedHours = null;
        this.tempPriority = null;
        this.tempAssigneeId = null;
    }

    /**
     * Reset sprint creation state
     */
    public void resetSprintCreation() {
        this.sprintCreationMode = false;
        this.sprintCreationStage = null;
        this.tempSprintName = null;
        this.tempSprintDescription = null;
        this.tempSprintStartDate = null;
        this.tempSprintEndDate = null;
    }

    /**
     * Reset task completion state
     */
    public void resetTaskCompletion() {
        this.taskCompletionMode = false;
        this.taskCompletionStage = null;
        this.tempTaskId = 0;
        this.tempActualHours = null;
    }

    /**
     * Reset assign to sprint state
     */
    public void resetAssignToSprint() {
        this.assignToSprintMode = false;
        this.assignToSprintStage = null;
        this.tempSprintId = null;
    }

    /**
     * Reset start task work state
     */
    public void resetStartTaskWork() {
        this.startTaskWorkMode = false;
        this.startTaskWorkStage = null;
    }

    // Getters and setters for all fields

    public boolean isSprintCreationMode() {
        return sprintCreationMode;
    }

    public void setSprintCreationMode(boolean sprintCreationMode) {
        this.sprintCreationMode = sprintCreationMode;
        if (sprintCreationMode) {
            this.newTaskMode = false;
            this.taskCompletionMode = false;
            this.assignToSprintMode = false;
            this.startTaskWorkMode = false;
            this.viewingTaskMode = false;
            this.endSprintMode = false;
        }
    }

    public String getSprintCreationStage() {
        return sprintCreationStage;
    }

    public void setSprintCreationStage(String sprintCreationStage) {
        this.sprintCreationStage = sprintCreationStage;
        if (sprintCreationStage != null) {
            this.sprintCreationMode = true;
        }
    }

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

    public boolean isEndSprintMode() {
        return endSprintMode;
    }

    public void setEndSprintMode(boolean endSprintMode) {
        this.endSprintMode = endSprintMode;
        if (endSprintMode) {
            this.newTaskMode = false;
            this.taskCompletionMode = false;
            this.assignToSprintMode = false;
            this.startTaskWorkMode = false;
            this.viewingTaskMode = false;
            this.sprintCreationMode = false;
        }
    }

    public Long getTempAssigneeId() {
        return tempAssigneeId;
    }

    public void setTempAssigneeId(Long tempAssigneeId) {
        this.tempAssigneeId = tempAssigneeId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.employeeId = user.getEmployeeId();
        }
    }

    public boolean isNewTaskMode() {
        return newTaskMode;
    }

    public void setNewTaskMode(boolean newTaskMode) {
        this.newTaskMode = newTaskMode;
        if (newTaskMode) {
            this.taskCompletionMode = false;
            this.assignToSprintMode = false;
            this.startTaskWorkMode = false;
            this.viewingTaskMode = false;
        }
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

    public String getTempPriority() {
        return tempPriority;
    }

    public void setTempPriority(String tempPriority) {
        this.tempPriority = tempPriority;
    }

    public boolean isTaskCompletionMode() {
        return taskCompletionMode;
    }

    public void setTaskCompletionMode(boolean taskCompletionMode) {
        this.taskCompletionMode = taskCompletionMode;
        if (taskCompletionMode) {
            this.newTaskMode = false;
            this.assignToSprintMode = false;
            this.startTaskWorkMode = false;
            this.viewingTaskMode = false;
        }
    }

    public String getTaskCompletionStage() {
        return taskCompletionStage;
    }

    public void setTaskCompletionStage(String taskCompletionStage) {
        this.taskCompletionStage = taskCompletionStage;
        if (taskCompletionStage != null) {
            this.taskCompletionMode = true;
        }
    }

    public int getTempTaskId() {
        return tempTaskId;
    }

    public void setTempTaskId(int tempTaskId) {
        this.tempTaskId = tempTaskId;
    }

    public Double getTempActualHours() {
        return tempActualHours;
    }

    public void setTempActualHours(Double tempActualHours) {
        this.tempActualHours = tempActualHours;
    }

    public boolean isAssignToSprintMode() {
        return assignToSprintMode;
    }

    public void setAssignToSprintMode(boolean assignToSprintMode) {
        this.assignToSprintMode = assignToSprintMode;
        if (assignToSprintMode) {
            this.newTaskMode = false;
            this.taskCompletionMode = false;
            this.startTaskWorkMode = false;
            this.viewingTaskMode = false;
        }
    }

    public String getAssignToSprintStage() {
        return assignToSprintStage;
    }

    public void setAssignToSprintStage(String assignToSprintStage) {
        this.assignToSprintStage = assignToSprintStage;
        if (assignToSprintStage != null) {
            this.assignToSprintMode = true;
        }
    }

    public Long getTempSprintId() {
        return tempSprintId;
    }

    public void setTempSprintId(Long tempSprintId) {
        this.tempSprintId = tempSprintId;
    }

    public boolean isStartTaskWorkMode() {
        return startTaskWorkMode;
    }

    public void setStartTaskWorkMode(boolean startTaskWorkMode) {
        this.startTaskWorkMode = startTaskWorkMode;
        if (startTaskWorkMode) {
            this.newTaskMode = false;
            this.taskCompletionMode = false;
            this.assignToSprintMode = false;
            this.viewingTaskMode = false;
        }
    }

    public String getStartTaskWorkStage() {
        return startTaskWorkStage;
    }

    public void setStartTaskWorkStage(String startTaskWorkStage) {
        this.startTaskWorkStage = startTaskWorkStage;
        if (startTaskWorkStage != null) {
            this.startTaskWorkMode = true;
        }
    }

    public boolean isViewingTaskMode() {
        return viewingTaskMode;
    }

    public void setViewingTaskMode(boolean viewingTaskMode) {
        this.viewingTaskMode = viewingTaskMode;
        if (viewingTaskMode) {
            this.newTaskMode = false;
            this.taskCompletionMode = false;
            this.assignToSprintMode = false;
            this.startTaskWorkMode = false;
        }
    }

    public Long getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(Long currentTaskId) {
        this.currentTaskId = currentTaskId;
        if (currentTaskId != null) {
            this.viewingTaskMode = true;
        }
    }
}