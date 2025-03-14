package com.springboot.MyTodoList.model.bot;

import com.springboot.MyTodoList.model.User;

/**
 * Represents the state of a user's interaction with the Telegram bot.
 * This class maintains authentication status and conversation context
 * for each user chatting with the bot.
 */
public class UserBotState {
    
    // Authentication state
    private boolean authenticated = false;
    private String employeeId = null;
    private User user = null;
    
    // Conversation state flags
    private boolean newTaskMode = false;
    private boolean viewingTaskMode = false;
    private Long currentTaskId = null;
    
    // Temporary data storage for multi-step operations
    private String tempTaskTitle = null;
    private String tempTaskDescription = null;
    
    /**
     * Resets the user's state to unauthenticated with no active operations.
     * This is typically called when starting a new conversation or when
     * the user issues a command to return to the main menu.
     */
    public void reset() {
        this.authenticated = false;
        this.employeeId = null;
        this.user = null;
        this.newTaskMode = false;
        this.viewingTaskMode = false;
        this.currentTaskId = null;
        this.tempTaskTitle = null;
        this.tempTaskDescription = null;
    }
    
    // Getters and setters
    
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
        // Clear any conflicting modes
        if (newTaskMode) {
            this.viewingTaskMode = false;
            this.currentTaskId = null;
        }
    }
    
    public boolean isViewingTaskMode() {
        return viewingTaskMode;
    }
    
    public void setViewingTaskMode(boolean viewingTaskMode) {
        this.viewingTaskMode = viewingTaskMode;
        // Clear any conflicting modes
        if (viewingTaskMode) {
            this.newTaskMode = false;
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
}