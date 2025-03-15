package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.KpiData;
import com.springboot.MyTodoList.service.KpiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/kpi")
public class KpiController {

    @Autowired
    private KpiService kpiService;
    
    /**
     * Get KPIs for a user in a date range
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<KpiData> getUserKpi(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        // Default to last 30 days if no dates provided
        if (startDate == null) {
            startDate = OffsetDateTime.now().minusDays(30);
        }
        
        if (endDate == null) {
            endDate = OffsetDateTime.now();
        }
        
        KpiData kpiData = kpiService.calculateUserKpis(userId, startDate, endDate);
        return ResponseEntity.ok(kpiData);
    }
    
    /**
     * Get KPIs for a team in a date range
     */
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<KpiData> getTeamKpi(
            @PathVariable Long teamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        
        // Default to last 30 days if no dates provided
        if (startDate == null) {
            startDate = OffsetDateTime.now().minusDays(30);
        }
        
        if (endDate == null) {
            endDate = OffsetDateTime.now();
        }
        
        KpiData kpiData = kpiService.calculateTeamKpis(teamId, startDate, endDate);
        return ResponseEntity.ok(kpiData);
    }
}