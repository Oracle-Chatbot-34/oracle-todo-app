package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.KpiResult;
import com.springboot.MyTodoList.service.KpiGraphQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller
public class KpiGraphQLController {

    @Autowired
    private KpiGraphQLService kpiGraphQLService;

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public KpiResult getKpiData(
            @Argument Long startSprintId,
            @Argument Long endSprintId) {
        
        // Only validate that startSprintId is provided
        if (startSprintId == null) {
            throw new IllegalArgumentException("startSprintId is required");
        }
        
        // Always pass null for userId and teamId, with allUsers=true
        return kpiGraphQLService.generateKpiResult(startSprintId, endSprintId);
    }
}