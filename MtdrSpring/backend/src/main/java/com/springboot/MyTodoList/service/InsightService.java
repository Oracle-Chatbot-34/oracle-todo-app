package com.springboot.MyTodoList.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.lang.InterruptedException;

@Service
public class InsightService {

    private static final Logger logger = LoggerFactory.getLogger(InsightService.class);

    @Value("${gemini.project-id}")
    private String projectId;

    @Value("${gemini.location}")
    private String location;

    // Use instance variable for VertexAI if service is a singleton (@Service)
    // This avoids creating a new VertexAI client on every method call, which is
    // inefficient
    // Ensure the close() method is called on application shutdown
    private VertexAI vertexAI;

    // Initialize VertexAI when the service is created
    public InsightService(@Value("${gemini.project-id:dashmaster-project}") String projectId,
            @Value("${gemini.location:us-central1}") String location) {
        this.projectId = projectId;
        this.location = location;
        try {
            // Use the standard constructor relying on Application Default Credentials (ADC)
            this.vertexAI = new VertexAI(projectId, location);
        } catch (Exception e) {
            logger.error("Failed to initialize VertexAI client", e);
            // Handle initialization failure but continue
            logger.warn("Will use backup insights service without Gemini AI");
        }
    }

    public void closeVertexAI() {
        if (this.vertexAI != null) {
            try {
                this.vertexAI.close();
                logger.info("VertexAI client closed.");
            } catch (Exception e) {
                logger.error("Error closing VertexAI client", e);
            }
        }
    }

    public String generateInsights(Map<String, Object> kpiData, boolean isIndividual) {
        if (kpiData == null) {
            return "No data available for insights.";
        }

        // If VertexAI failed to initialize, use backup insights
        if (vertexAI == null) {
            return generateFallbackInsights(kpiData, isIndividual);
        }

        try {
            // Get the GenerativeModel from the initialized VertexAI instance
            // Creating the model object is lightweight
            GenerativeModel model = new GenerativeModel("gemini-1.5-pro", vertexAI);

            StringBuilder prompt = new StringBuilder();
            prompt.append("You are an expert data analyst specializing in software development metrics. ");
            prompt.append("Analyze the following KPI data and provide meaningful insights that would be valuable for ");

            if (isIndividual) {
                prompt.append("a developer to improve their performance. ");
            } else {
                prompt.append("a team manager to improve team performance. ");
            }

            prompt.append("Focus on patterns, areas of improvement, and concrete actionable recommendations. ");
            prompt.append("Data: ").append(kpiData.toString()).append("\n\n");

            prompt.append("Format your response in these sections:\n");
            prompt.append("1. SUMMARY: A brief summary of the overall performance\n");
            prompt.append("2. STRENGTHS: Key areas where performance is good\n");
            prompt.append("3. OPPORTUNITIES: Areas that need improvement\n");
            prompt.append("4. RECOMMENDATIONS: Specific actionable steps to improve\n\n");

            prompt.append("Keep the entire response under 300 words and focus on being specific and actionable.");

            try {
                // Use the asynchronous method
                ApiFuture<GenerateContentResponse> responseFuture = model.generateContentAsync(prompt.toString());

                // Block and get the result from the ApiFuture
                GenerateContentResponse response = responseFuture.get(); // This will wait for the AI response

                // Extract the text content from the response
                if (response != null && !response.getCandidatesList().isEmpty()) {
                    // Assuming the first candidate and first part contain text content
                    // The structure might vary, inspect the response object for complex output
                    if (!response.getCandidatesList().get(0).getContent().getPartsList().isEmpty()) {
                        return response.getCandidatesList().get(0).getContent().getPartsList().get(0).getText();
                    }
                }

                // Handle cases where no text content is generated or response is empty
                return "Unable to generate meaningful insights from the data.";

            } catch (InterruptedException | ExecutionException e) {
                // Log the error properly
                logger.error("Failed to generate insights from Vertex AI", e);
                // Return a user-friendly error message
                return "Unable to generate insights at this time due to an AI service error.";
            } catch (Exception e) {
                // Catch any other unexpected exceptions
                logger.error("An unexpected error occurred while generating insights", e);
                return "An unexpected error occurred.";
            }
        } catch (Exception e) {
            logger.error("Error initializing Generative AI model", e);
            return generateFallbackInsights(kpiData, isIndividual);
        }
    }

    /**
     * Generate fallback insights without using AI
     */
    private String generateFallbackInsights(Map<String, Object> kpiData, boolean isIndividual) {
        StringBuilder insights = new StringBuilder();

        // Extract key metrics
        Integer totalTasks = (Integer) kpiData.getOrDefault("totalTasks", 0);
        Integer completedTasks = (Integer) kpiData.getOrDefault("completedTasks", 0);
        Double completionRate = kpiData.containsKey("completionRate") ? (Double) kpiData.get("completionRate")
                : (totalTasks > 0 ? (double) completedTasks / totalTasks * 100 : 0);

        Double estimatedHours = (Double) kpiData.getOrDefault("totalEstimatedHours", 0.0);
        Double actualHours = (Double) kpiData.getOrDefault("totalActualHours", 0.0);
        Double timeEfficiency = estimatedHours > 0 ? (estimatedHours / actualHours) * 100 : 0;

        // Generate basic insights
        insights.append("SUMMARY: ");
        if (isIndividual) {
            insights.append("Completed ").append(completedTasks).append(" out of ").append(totalTasks)
                    .append(" tasks (").append(String.format("%.1f", completionRate)).append("%) ");
        } else {
            insights.append("Team completed ").append(completedTasks).append(" out of ").append(totalTasks)
                    .append(" tasks (").append(String.format("%.1f", completionRate)).append("%) ");
        }
        insights.append("with a time efficiency of ").append(String.format("%.1f", timeEfficiency)).append("%.\n\n");

        insights.append("STRENGTHS: ");
        if (completionRate >= 80) {
            insights.append("High task completion rate. ");
        }
        if (timeEfficiency >= 90) {
            insights.append("Excellent time management. ");
        }
        insights.append("\n\n");

        insights.append("OPPORTUNITIES: ");
        if (completionRate < 80) {
            insights.append("Improve task completion rate. ");
        }
        if (timeEfficiency < 90) {
            insights.append("Enhance time estimation accuracy. ");
        }
        insights.append("\n\n");

        insights.append("RECOMMENDATIONS: ");
        if (isIndividual) {
            insights.append("Focus on completing key deliverables. Break down complex tasks. ");
            if (timeEfficiency < 90) {
                insights.append("Revise estimation techniques for future tasks. ");
            }
        } else {
            insights.append("Ensure proper task distribution. Implement regular check-ins. ");
            if (timeEfficiency < 90) {
                insights.append("Conduct team estimation workshops. ");
            }
        }

        return insights.toString();
    }
}