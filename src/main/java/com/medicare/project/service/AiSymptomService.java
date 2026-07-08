package com.medicare.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * AI-powered symptom analyzer using Claude (Anthropic API).
 * Falls back to a rule-based keyword engine if API key is not configured.
 *
 * Interview explanation:
 * "Calls Claude via REST HTTP. If ANTHROPIC_API_KEY is not set, uses
 *  a keyword-matching fallback engine with 11 medical rules."
 */
@Service
public class AiSymptomService {

    @Value("${ANTHROPIC_API_KEY:}")
    private String anthropicApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===================== KEYWORD FALLBACK ENGINE =====================

    private static class Rule {
        final String condition, specialization, urgency, advice, department;
        final List<String> keywords;

        Rule(String condition, String specialization, String urgency,
             String advice, String department, String... keywords) {
            this.condition = condition;
            this.specialization = specialization;
            this.urgency = urgency;
            this.advice = advice;
            this.department = department;
            this.keywords = Arrays.asList(keywords);
        }
    }

    private static final List<Rule> RULES = List.of(
        new Rule("Cardiac / Heart-related issue", "Cardiologist", "Emergency",
            "Sit down, stay calm and avoid physical exertion. If chest pain lasts more than a few minutes, call emergency services immediately.",
            "Cardiology",
            "chest pain", "chest tightness", "heart", "palpitation", "shortness of breath", "left arm pain"),

        new Rule("Neurological issue", "Neurologist", "High",
            "Rest in a quiet, dark room. Avoid screens. If you experience sudden weakness, slurred speech or vision loss, seek emergency care.",
            "Neurology",
            "severe headache", "migraine", "dizziness", "seizure", "numbness", "memory loss", "fainting"),

        new Rule("Respiratory infection", "General Physician", "Medium",
            "Stay hydrated, take steam inhalation and rest. Avoid cold food and smoking. See a doctor if fever persists more than 3 days.",
            "General Medicine",
            "cough", "cold", "breathing difficulty", "wheezing", "asthma", "chest congestion", "sore throat"),

        new Rule("Digestive / Gastro issue", "General Physician", "Medium",
            "Eat light, avoid oily and spicy food. Drink ORS to prevent dehydration. See a doctor if symptoms persist more than 24 hours.",
            "General Medicine",
            "stomach pain", "abdominal pain", "vomiting", "diarrhea", "acidity", "gas", "indigestion", "nausea"),

        new Rule("Skin condition", "Dermatologist", "Low",
            "Keep the affected area clean and dry. Avoid scratching. Do not apply unknown creams without a doctor's advice.",
            "Dermatology",
            "skin", "rash", "itching", "acne", "pimple", "allergy", "eczema"),

        new Rule("Orthopedic / Bone-joint issue", "Orthopedic Surgeon", "Medium",
            "Rest the affected joint, apply an ice pack for swelling, and avoid heavy lifting until you consult a doctor.",
            "Orthopedics",
            "joint pain", "back pain", "knee pain", "fracture", "sprain", "bone", "muscle pain", "neck pain"),

        new Rule("ENT (Ear / Nose / Throat) issue", "ENT Specialist", "Medium",
            "Avoid inserting anything into the ear. Warm salt-water gargle helps sore throat. See a doctor if pain worsens.",
            "ENT",
            "ear pain", "hearing loss", "tinnitus", "sinus", "nose bleed", "tonsil", "ear", "nose"),

        new Rule("Women's health issue", "Gynecologist", "Medium",
            "Keep a record of your symptoms and their frequency. Consult a Gynecologist for proper evaluation.",
            "Gynecology",
            "menstrual", "pregnancy", "vaginal", "pelvic pain", "ovarian", "uterus", "gynecology"),

        new Rule("Child health issue", "Pediatrician", "Medium",
            "Monitor the child's temperature and hydration. Do not give adult medications to children. Consult a Pediatrician.",
            "Pediatrics",
            "child", "infant", "baby", "toddler", "pediatric", "vaccination", "growth"),

        new Rule("General fever / infection", "General Physician", "Medium",
            "Take rest, drink plenty of fluids and monitor your temperature. Consult a doctor if fever lasts more than 3 days.",
            "General Medicine",
            "fever", "body pain", "chills", "weakness", "tiredness", "fatigue", "infection")
    );

    private static final Rule DEFAULT_RULE = new Rule(
        "General consultation recommended", "General Physician", "Low",
        "Your symptoms are not specific. Please consult a General Physician who can examine you and refer to a specialist if needed.",
        "General Medicine");

    // ===================== MAIN ENTRY POINT =====================

    public Map<String, String> analyzeSymptoms(String symptoms) {
        if (symptoms == null || symptoms.isBlank()) {
            return keywordAnalysis(symptoms);
        }

        // Try Claude API if API key is configured
        if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
            try {
                return claudeAnalysis(symptoms);
            } catch (Exception e) {
                // Fall back to keyword engine silently
            }
        }

        return keywordAnalysis(symptoms);
    }

    // ===================== CLAUDE API CALL =====================

    private Map<String, String> claudeAnalysis(String symptoms) throws Exception {
        String prompt = "You are a medical symptom analyzer for a hospital system. "
            + "Based on the patient's symptoms, provide a structured analysis.\n\n"
            + "Available departments: Cardiology, Neurology, Orthopedics, Pediatrics, Dermatology, General Medicine, ENT, Gynecology\n\n"
            + "Patient symptoms: " + symptoms + "\n\n"
            + "Respond ONLY with a valid JSON object (no markdown, no explanation) in this exact format:\n"
            + "{\n"
            + "  \"condition\": \"brief condition description\",\n"
            + "  \"specialization\": \"type of specialist needed\",\n"
            + "  \"department\": \"one of the departments listed above\",\n"
            + "  \"urgency\": \"Low or Medium or High or Emergency\",\n"
            + "  \"advice\": \"brief practical first-aid advice in 2-3 sentences\"\n"
            + "}";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-3-haiku-20240307");
        requestBody.put("max_tokens", 400);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", anthropicApiKey);
        headers.set("anthropic-version", "2023-06-01");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
            "https://api.anthropic.com/v1/messages", entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("content").get(0).path("text").asText();

        // Clean up in case Claude wraps in markdown
        content = content.trim();
        if (content.startsWith("```")) {
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        }

        JsonNode parsed = objectMapper.readTree(content);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("condition",      parsed.path("condition").asText("General consultation recommended"));
        result.put("specialization", parsed.path("specialization").asText("General Physician"));
        result.put("department",     parsed.path("department").asText("General Medicine"));
        result.put("urgency",        parsed.path("urgency").asText("Medium"));
        result.put("advice",         parsed.path("advice").asText("Please consult a doctor."));
        result.put("matched",        "");
        result.put("success",        "true");
        result.put("source",         "ai");
        return result;
    }

    // ===================== KEYWORD ENGINE =====================

    private Map<String, String> keywordAnalysis(String symptoms) {
        String input = symptoms == null ? "" : symptoms.toLowerCase();
        Rule bestRule = DEFAULT_RULE;
        int bestScore = 0;
        List<String> matchedKeywords = new ArrayList<>();

        for (Rule rule : RULES) {
            int score = 0;
            List<String> matches = new ArrayList<>();
            for (String kw : rule.keywords) {
                if (input.contains(kw)) { score++; matches.add(kw); }
            }
            if (score > bestScore) {
                bestScore = score;
                bestRule = rule;
                matchedKeywords = matches;
            }
        }

        Map<String, String> result = new LinkedHashMap<>();
        result.put("condition",      bestRule.condition);
        result.put("specialization", bestRule.specialization);
        result.put("department",     bestRule.department);
        result.put("urgency",        bestRule.urgency);
        result.put("advice",         bestRule.advice);
        result.put("matched",        String.join(", ", matchedKeywords));
        result.put("success",        "true");
        result.put("source",         "keyword");
        return result;
    }
}
