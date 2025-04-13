package com.springboot.MyTodoList.service;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class InsightService {

    private static final Logger logger = LoggerFactory.getLogger(InsightService.class);
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.project-id}")
    private String projectId;
    
    @Value("${gemini.location}")
    private String location;
    
    private GenerativeModel getGenerativeModel() {
        try (VertexAI vertexAI = new VertexAI(projectId, location, apiKey)) {
            return new GenerativeModel("gemini-1.5-pro", vertexAI);
        } catch (IOException e) {
            logger.error("Failed to initialize Gemini model", e);
            return null;
        }
    }
    
    public String generateInsights(Map<String, Object> kpiData, boolean isIndividual) {
        if (kpiData == null) {
            return "No data available for insights.";
        }
        
        GenerativeModel model = getGenerativeModel();
        if (model == null) {
            return "Unable to generate insights due to AI service unavailability.";
        }
        
        try {
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
            
            CompletableFuture<String> responseFuture = model.generateContent(prompt.toString())
                    .thenApply(ResponseHandler::getContent);
            
            return responseFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to generate insights", e);
            return "Unable to generate insights at this time.";
        }
    }
}