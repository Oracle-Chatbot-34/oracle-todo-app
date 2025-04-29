package com.springboot.MyTodoList.dto;

public class SprintDataForPie {
    private Integer id;
    private String name;
    private Integer count;

    public SprintDataForPie() {
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}