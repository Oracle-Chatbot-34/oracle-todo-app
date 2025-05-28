package com.springboot.MyTodoList.controller;

import com.springboot.MyTodoList.dto.ReportRequest;
import com.springboot.MyTodoList.dto.SprintTasksReport;
import com.springboot.MyTodoList.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * Generate a report based on the provided request parameters
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateReport(@RequestBody ReportRequest request) {
        Map<String, Object> reportData = reportService.generateReport(request);
        return ResponseEntity.ok(reportData);
    }

    /**
     * Get tasks from the last sprint grouped by developer
     */
    @GetMapping("/sprint-tasks")
    public ResponseEntity<SprintTasksReport> getLastSprintTasks(
            @RequestParam(required = false) Long sprintId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long teamId) {
        SprintTasksReport report = reportService.getLastSprintTasksReport(sprintId, userId, teamId);
        return ResponseEntity.ok(report);
    }
}