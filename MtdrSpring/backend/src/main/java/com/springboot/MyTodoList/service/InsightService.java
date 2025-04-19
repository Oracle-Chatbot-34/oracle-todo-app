package com.springboot.MyTodoList.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InsightService {

    private static final Logger logger = LoggerFactory.getLogger(InsightService.class);

    @Value("${gemini.project-id:dashmaster-project}")
    private String projectId;

    @Value("${gemini.location:us-central1}")
    private String location;

    // We're not initializing VertexAI at all to avoid the error

    /**
     * Generate insights based on task data
     * 
     * This implementation provides pre-defined insights without using AI
     * since Google Cloud authentication isn't set up
     */
    public String generateInsights(Map<String, Object> kpiData, boolean isIndividual) {
        if (kpiData == null) {
            return "No data available for insights.";
        }

        logger.info("Generating insights using fallback method (no AI)");
        return generateFallbackInsights(kpiData, isIndividual);
    }

    /**
     * Generate fallback insights without using AI
     */
    private String generateFallbackInsights(Map<String, Object> kpiData, boolean isIndividual) {
        StringBuilder insights = new StringBuilder();

        // Extract key metrics
        Number totalTasks = (Number) kpiData.getOrDefault("totalTasks", 0);
        Number completedTasks = (Number) kpiData.getOrDefault("completedTasks", 0);

        double completionRateValue = 0;
        if (kpiData.containsKey("completionRate")) {
            completionRateValue = ((Number) kpiData.get("completionRate")).doubleValue();
        } else if (totalTasks.intValue() > 0) {
            completionRateValue = (double) completedTasks.intValue() / totalTasks.intValue() * 100;
        }

        Number totalEstimatedHours = (Number) kpiData.getOrDefault("totalEstimatedHours", 0);
        Number totalActualHours = (Number) kpiData.getOrDefault("totalActualHours", 0);

        double timeEfficiencyValue = 0;
        if (totalActualHours.doubleValue() > 0) {
            timeEfficiencyValue = (totalEstimatedHours.doubleValue() / totalActualHours.doubleValue()) * 100;
        }

        // Generate basic insights
        insights.append("SUMMARY: ");
        if (isIndividual) {
            insights.append("Completed ").append(completedTasks).append(" out of ").append(totalTasks)
                    .append(" tasks (").append(String.format("%.1f", completionRateValue)).append("%) ");
        } else {
            insights.append("Team completed ").append(completedTasks).append(" out of ").append(totalTasks)
                    .append(" tasks (").append(String.format("%.1f", completionRateValue)).append("%) ");
        }
        insights.append("with a time efficiency of ").append(String.format("%.1f", timeEfficiencyValue))
                .append("%.\n\n");

        insights.append("STRENGTHS: ");
        if (completionRateValue >= 80) {
            insights.append("High task completion rate demonstrates excellent progress. ");
        }
        if (timeEfficiencyValue >= 90) {
            insights.append("Excellent time management and estimation accuracy. ");
        }
        insights.append("\n\n");

        insights.append("OPPORTUNITIES: ");
        if (completionRateValue < 80) {
            insights.append("Improve task completion rate by addressing blockers early. ");
        }
        if (timeEfficiencyValue < 90) {
            insights.append("Enhance time estimation accuracy through better task breakdown. ");
        }
        insights.append("\n\n");

        insights.append("RECOMMENDATIONS: ");
        if (isIndividual) {
            insights.append("Focus on completing key deliverables and breaking down complex tasks. ");
            if (timeEfficiencyValue < 90) {
                insights.append(
                        "Track time more precisely and revise estimation techniques based on historical data. ");
            }
        } else {
            insights.append("Ensure proper task distribution and implement regular check-ins. ");
            if (timeEfficiencyValue < 90) {
                insights.append("Conduct team estimation workshops to align on complexity assessment. ");
            }
        }

        return insights.toString();
    }
}