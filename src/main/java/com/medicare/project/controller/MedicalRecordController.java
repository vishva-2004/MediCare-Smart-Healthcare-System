package com.medicare.project.controller;

import com.medicare.project.entity.*;
import com.medicare.project.repository.*;
import com.medicare.project.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/medical-record")
public class MedicalRecordController {

    @Autowired private MedicalRecordRepository medicalRecordRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    // Doctor adds record + completes appointment in one action
    @PostMapping("/complete-visit")
    public ResponseEntity<?> completeVisit(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        Long appointmentId = Long.parseLong(body.get("appointmentId").toString());
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        String diagnosis    = body.getOrDefault("diagnosis", "").toString();
        String prescription = body.getOrDefault("prescription", "").toString();
        String notes        = body.getOrDefault("notes", "").toString();

        MedicalRecord record = new MedicalRecord();
        record.setPatient(appointment.getPatient());
        record.setDoctor(doctor);
        record.setAppointment(appointment);
        record.setDiagnosis(diagnosis);
        record.setPrescription(prescription);
        record.setNotes(notes);
        medicalRecordRepository.save(record);

        appointment.setStatus("COMPLETED");
        appointmentRepository.save(appointment);

        if (appointment.getPatient().getUser() != null) {
            notificationService.notifyUser(appointment.getPatient().getUser().getId(),
                "📋 Dr. " + doctor.getName() + " has completed your visit and added your prescription. Check My Records.");
        }

        return ResponseEntity.ok(Map.of("message", "Visit completed and prescription saved successfully."));
    }

    // Doctor adds record (standalone, without completing appointment)
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));

        Long patientId = Long.parseLong(body.get("patientId").toString());
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        MedicalRecord record = new MedicalRecord();
        record.setPatient(patient);
        record.setDoctor(doctor);
        record.setDiagnosis(body.getOrDefault("diagnosis", "").toString());
        record.setPrescription(body.getOrDefault("prescription", "").toString());
        record.setNotes(body.getOrDefault("notes", "").toString());

        if (body.get("appointmentId") != null) {
            appointmentRepository.findById(Long.parseLong(body.get("appointmentId").toString()))
                    .ifPresent(record::setAppointment);
        }
        medicalRecordRepository.save(record);

        if (patient.getUser() != null) {
            notificationService.notifyUser(patient.getUser().getId(),
                "📋 Dr. " + doctor.getName() + " has added a medical record for your visit. Check your records.");
        }

        return ResponseEntity.ok(Map.of("message", "Medical record saved successfully."));
    }

    @GetMapping("/my")
    public ResponseEntity<?> myRecords(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));
        return ResponseEntity.ok(toDto(medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patient.getId())));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(toDto(medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patientId)));
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(toDto(medicalRecordRepository.findAll()));
    }

    private List<Map<String, Object>> toDto(List<MedicalRecord> records) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MedicalRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("patientName", r.getPatient().getName());
            m.put("doctorName", r.getDoctor().getName());
            m.put("specialization", r.getDoctor().getSpecialization());
            m.put("diagnosis", r.getDiagnosis());
            m.put("prescription", r.getPrescription());
            m.put("notes", r.getNotes());
            m.put("recordDate", r.getRecordDate());
            if (r.getAppointment() != null) {
                m.put("appointmentDate", r.getAppointment().getAppointmentDate());
            }
            result.add(m);
        }
        return result;
    }
}
