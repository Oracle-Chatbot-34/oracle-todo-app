package com.springboot.MyTodoList.model;

public enum TaskStatus {
    SELECTED_FOR_DEVELOPMENT("Selected for Development"),
    IN_PROGRESS("In Progress"),
    DELAYED("Delayed"),
    IN_QA("In QA"),
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