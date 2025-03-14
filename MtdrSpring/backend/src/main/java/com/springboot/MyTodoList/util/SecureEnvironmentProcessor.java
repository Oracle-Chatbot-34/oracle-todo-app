package com.springboot.MyTodoList.util;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SecureEnvironmentProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Location of secure environment file
        String envFilePath = System.getProperty("user.home") + "/.dashmaster/.env";
        
        try {
            File envFile = new File(envFilePath);
            if (envFile.exists()) {
                Properties props = new Properties();
                Resource resource = new FileSystemResource(envFile);
                
                String content = StreamUtils.copyToString(
                    resource.getInputStream(), StandardCharsets.UTF_8);
                
                // Parse the .env file format
                for (String line : content.split("\n")) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        int separatorIndex = line.indexOf('=');
                        if (separatorIndex > 0) {
                            String key = line.substring(0, separatorIndex).trim();
                            String value = line.substring(separatorIndex + 1).trim();
                            // Remove quotes if present
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            props.setProperty(key, value);
                        }
                    }
                }
                
                // Add properties to the environment
                PropertiesPropertySource propertySource = 
                    new PropertiesPropertySource("secureEnvironment", props);
                environment.getPropertySources().addFirst(propertySource);
            }
        } catch (IOException e) {
            System.err.println("Error loading secure environment: " + e.getMessage());
        }
    }
}