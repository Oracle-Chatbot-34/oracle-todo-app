package com.springboot.MyTodoList.dto;

public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String error;

    // Constructor for success response
    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.error = null;
    }

    // Constructor for error response
    public ApiResponse(String error) {
        this.success = false;
        this.data = null;
        this.error = error;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}