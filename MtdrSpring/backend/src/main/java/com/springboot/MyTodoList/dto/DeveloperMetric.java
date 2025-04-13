package com.springboot.MyTodoList.dto;

import java.util.List;

public class DeveloperMetric {
    private Long developerId;
    private String developerName;
    private List<Double> values;
    private List<String> sprints;

    public Long getDeveloperId() {
        return developerId;
    }

    public void setDeveloperId(Long developerId) {
        this.developerId = developerId;
    }

    public String getDeveloperName() {
        return developerName;
    }

    public void setDeveloperName(String developerName) {
        this.developerName = developerName;
    }

    public List<Double> getValues() {
        return values;
    }

    public void setValues(List<Double> values) {
        this.values = values;
    }

    public List<String> getSprints() {
        return sprints;
    }

    public void setSprints(List<String> sprints) {
        this.sprints = sprints;
    }
}