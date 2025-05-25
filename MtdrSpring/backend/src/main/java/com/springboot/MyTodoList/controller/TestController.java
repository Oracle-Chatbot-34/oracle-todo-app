package com.springboot.MyTodoList.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
public class TestController {

    @GetMapping("/api/test/zap")
    public String testEndpoint() {
        return "This endpoint is for ZAP security testing";
    }

    // Endpoint with XSS vulnerability
    @GetMapping("/api/test/zap/xss")
    public ResponseEntity<String> xssVulnerableEndpoint(@RequestParam String input) {
        // Deliberately unsafe - directly reflects user input into the response
        String htmlResponse = "<html><body><h1>Hello, " + input + "!</h1></body></html>";
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(htmlResponse);
    }

    // Endpoint with security header issues
    @GetMapping("/api/test/zap/headers")
    public ResponseEntity<String> insecureHeadersEndpoint() {
        // Missing important security headers
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=31536000")
                .body("This endpoint has missing security headers");
    }

    // Endpoint with information disclosure
    @GetMapping("/api/test/zap/info")
    public ResponseEntity<String> informationDisclosureEndpoint(@RequestParam String input)  {
        // Deliberately exposes server information
        String htmlResponse = "<html><body><h1>leaking information for testing, " + input + "!</h1></body></html>";

        return ResponseEntity.ok()
                .header("Server", "Apache Tomcat/9.0.44")
                .header("X-Powered-By", "Spring Boot 2.6.4")
                .body(htmlResponse);
    }
}