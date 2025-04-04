package com.springboot.MyTodoList.model;

public enum TaskStatus {
    BACKLOG("Backlog"),
    SELECTED_FOR_DEVELOPMENT("Selected for Development"),
    IN_PROGRESS("In Progress"),
    IN_SPRINT("In Sprint"),
    DELAYED("Delayed"),
    IN_QA("In QA"),
    COMPLETED("Completed"),
    DONE("Done");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static TaskStatus fromString(String text) {
        for (TaskStatus status : TaskStatus.values()) {
            if (status.name().equalsIgnoreCase(text) || 
                status.displayName.equalsIgnoreCase(text) ||
                status.name().replace("_", " ").equalsIgnoreCase(text)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No constant with text " + text + " found");
    }
}