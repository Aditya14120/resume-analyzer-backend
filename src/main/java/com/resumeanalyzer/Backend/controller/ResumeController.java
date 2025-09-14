package com.resumeanalyzer.Backend.controller;

import com.resumeanalyzer.Backend.service.ResumeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@CrossOrigin(origins = "*") // Allow frontend to connect
public class ResumeController {

    private final ResumeService resumeService;

    @Value("${google.api.key}")
    private String apiKey;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeResume(@RequestParam("resume") MultipartFile file) {
        try {
            Map<String, Object> result = resumeService.analyzeResume(file, apiKey);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
