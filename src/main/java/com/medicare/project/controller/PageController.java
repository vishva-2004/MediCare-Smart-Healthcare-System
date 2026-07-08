package com.medicare.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/home"})
    public String home() { return "home"; }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String register() { return "register"; }

    @GetMapping("/forgot-password")
    public String forgotPassword() { return "forgot-password"; }

    @GetMapping("/change-password")
    public String changePassword() { return "change-password"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "dashboard"; }

    @GetMapping("/patient-dashboard")
    public String patientDashboard() { return "patient-dashboard"; }

    @GetMapping("/doctor-dashboard")
    public String doctorDashboard() { return "doctor-dashboard"; }

    @GetMapping("/doctors")
    public String doctors() { return "doctors"; }

    @GetMapping("/appointments")
    public String appointments() { return "appointments"; }

    @GetMapping("/patients")
    public String patients() { return "patients"; }

    @GetMapping("/medical-records")
    public String medicalRecords() { return "medical-records"; }

    @GetMapping("/symptom-checker")
    public String symptomChecker() { return "symptom-checker"; }
}
