package com.medicare.project.controller;

import com.medicare.project.service.AiSymptomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired private AiSymptomService aiSymptomService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeSymptoms(@RequestBody Map<String, String> body) {
        String symptoms = body.get("symptoms");
        if (symptoms == null || symptoms.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Please describe your symptoms."));
        }
        if (symptoms.length() > 1000) {
            return ResponseEntity.badRequest().body(Map.of("message", "Symptoms description too long. Please be concise."));
        }
        Map<String, String> result = aiSymptomService.analyzeSymptoms(symptoms);
        return ResponseEntity.ok(result);
    }
}
