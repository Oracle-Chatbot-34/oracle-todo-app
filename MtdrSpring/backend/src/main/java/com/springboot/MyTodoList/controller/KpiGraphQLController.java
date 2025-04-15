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
            @Argument Long userId,
            @Argument Long teamId,
            @Argument Boolean allUsers,
            @Argument Long startSprintId,
            @Argument Long endSprintId) {
        
        // Modified validation - allow allUsers flag as a valid option
        if (userId == null && teamId == null && (allUsers == null || !allUsers)) {
            throw new IllegalArgumentException("Either userId, teamId, or allUsers must be provided");
        }
        
        // Validate that startSprintId is provided
        if (startSprintId == null) {
            throw new IllegalArgumentException("startSprintId is required");
        }
        
        return kpiGraphQLService.generateKpiResult(userId, teamId, allUsers, startSprintId, endSprintId);
    }
}