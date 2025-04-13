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
    // This avoids creating a new VertexAI client on every method call, which is inefficient
    // Ensure the close() method is called on application shutdown
    private VertexAI vertexAI;

    // Initialize VertexAI when the service is created
    public InsightService(@Value("${gemini.project-id}") String projectId, @Value("${gemini.location}") String location) {
         this.projectId = projectId;
         this.location = location;
         try {
             // Use the standard constructor relying on Application Default Credentials (ADC)
             this.vertexAI = new VertexAI(projectId, location);
         } catch (Exception e) {
             logger.error("Failed to initialize VertexAI client", e);
             // Handle initialization failure - maybe the application shouldn't start
             throw new RuntimeException("Failed to initialize VertexAI client", e);
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
        } finally {
             // No need to close model or vertexAI here if VertexAI is an instance variable
        }
    }
}