package com.medicare.project.controller;

import com.medicare.project.entity.*;
import com.medicare.project.repository.*;
import com.medicare.project.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Autowired private AppointmentRepository appointmentRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    private static final List<String> ALL_SLOTS = List.of(
        "09:00", "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00"
    );

    // Check available time slots for a doctor on a date
    @GetMapping("/slots")
    public ResponseEntity<?> getAvailableSlots(@RequestParam Long doctorId, @RequestParam String date) {
        try {
            LocalDate localDate = LocalDate.parse(date);
            List<Appointment> booked = appointmentRepository
                    .findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, localDate, "CANCELLED");
            List<String> bookedSlots = booked.stream()
                    .map(Appointment::getTimeSlot)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Map.of("bookedSlots", bookedSlots, "allSlots", ALL_SLOTS));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format."));
        }
    }

    // Patient books appointment
    @PostMapping("/book")
    public ResponseEntity<?> book(@RequestBody Map<String, Object> body, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Patient patient = patientRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Patient profile not found"));

        Long doctorId = Long.parseLong(body.get("doctorId").toString());
        String date   = body.get("appointmentDate").toString();
        String slot   = body.getOrDefault("timeSlot", "09:00").toString();
        String reason = body.getOrDefault("reason", "").toString();
        String symptoms = body.getOrDefault("symptoms", reason).toString();

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        // Reject past dates and past time slots on today's date
        LocalDate localDate = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        if (localDate.isBefore(today)) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "Cannot book appointments for a past date."));
        }
        if (localDate.isEqual(today)) {
            int slotHour    = Integer.parseInt(slot.split(":")[0]);
            int currentHour = LocalTime.now().getHour();
            if (slotHour <= currentHour) {
                return ResponseEntity.badRequest().body(Map.of("message",
                    "This time slot has already passed. Please select a future slot."));
            }
        }

        // Check if slot is already booked
        List<Appointment> existing = appointmentRepository
                .findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, localDate, "CANCELLED");
        boolean slotTaken = existing.stream()
                .anyMatch(a -> slot.equals(a.getTimeSlot()));
        if (slotTaken) {
            return ResponseEntity.badRequest().body(Map.of("message",
                "This time slot is already booked. Please choose another slot."));
        }

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setAppointmentDate(localDate);
        appointment.setTimeSlot(slot);
        appointment.setSymptoms(symptoms);
        appointment.setStatus("PENDING");
        appointmentRepository.save(appointment);

        // Notify doctor
        if (doctor.getUser() != null) {
            notificationService.notifyUser(doctor.getUser().getId(),
                "📅 New appointment from " + patient.getName()
                + " on " + date + " at " + slot + ". Please confirm.");
        }
        // Notify all admins
        notificationService.notifyAllAdmins(
            "📅 New appointment: " + patient.getName() + " → Dr. " + doctor.getName()
            + " on " + date + " at " + slot);

        return ResponseEntity.ok(Map.of("message",
            "Appointment booked for " + date + " at " + slot + ". Dr. " + doctor.getName() + " will confirm shortly."));
    }

    // Get my appointments (patient or doctor)
    @GetMapping("/my")
    public ResponseEntity<?> myAppointments(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        String role = user.getRoles().stream().map(r -> r.getName()).findFirst().orElse("");

        List<Appointment> appointments;
        if (role.equals("ROLE_PATIENT")) {
            Patient patient = patientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Patient profile not found"));
            appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateDesc(patient.getId());
        } else if (role.equals("ROLE_DOCTOR")) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
            appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateDesc(doctor.getId());
        } else {
            appointments = appointmentRepository.findAll();
        }
        return ResponseEntity.ok(toDto(appointments));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(toDto(appointmentRepository.findAll()));
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPending() {
        return ResponseEntity.ok(toDto(appointmentRepository.findByStatus("PENDING")));
    }

    @PostMapping("/confirm/{id}")
    public ResponseEntity<?> confirm(@PathVariable Long id, Authentication auth) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        a.setStatus("CONFIRMED");
        appointmentRepository.save(a);
        notificationService.notifyUser(a.getPatient().getUser().getId(),
            "✅ Your appointment with Dr. " + a.getDoctor().getName()
            + " on " + a.getAppointmentDate() + " at " + a.getTimeSlot() + " is confirmed!");
        return ResponseEntity.ok(Map.of("message", "Appointment confirmed."));
    }

    @PostMapping("/complete/{id}")
    public ResponseEntity<?> complete(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));
        a.setStatus("COMPLETED");
        if (body != null && body.get("notes") != null) a.setNotes(body.get("notes"));
        appointmentRepository.save(a);
        if (a.getPatient().getUser() != null) {
            notificationService.notifyUser(a.getPatient().getUser().getId(),
                "🏥 Your appointment with Dr. " + a.getDoctor().getName() + " is marked complete. Check your records.");
        }
        return ResponseEntity.ok(Map.of("message", "Appointment completed."));
    }

    @PostMapping("/cancel/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id, Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        // Patients can only cancel their own appointments
        String role = user.getRoles().stream().map(r -> r.getName()).findFirst().orElse("");
        if (role.equals("ROLE_PATIENT")) {
            Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);
            if (patient == null || !patient.getId().equals(a.getPatient().getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "You can only cancel your own appointments."));
            }
        }

        if ("COMPLETED".equals(a.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot cancel a completed appointment."));
        }

        a.setStatus("CANCELLED");
        appointmentRepository.save(a);

        if (a.getPatient().getUser() != null) {
            notificationService.notifyUser(a.getPatient().getUser().getId(),
                "❌ Your appointment with Dr. " + a.getDoctor().getName() + " on "
                + a.getAppointmentDate() + " has been cancelled.");
        }
        if (a.getDoctor().getUser() != null) {
            notificationService.notifyUser(a.getDoctor().getUser().getId(),
                "❌ Appointment with " + a.getPatient().getName() + " on "
                + a.getAppointmentDate() + " at " + a.getTimeSlot() + " was cancelled.");
        }
        return ResponseEntity.ok(Map.of("message", "Appointment cancelled. The slot is now available again."));
    }

    private List<Map<String, Object>> toDto(List<Appointment> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("patientName", a.getPatient().getName());
            m.put("patientId", a.getPatient().getId());
            m.put("doctorName", a.getDoctor().getName());
            m.put("doctorId", a.getDoctor().getId());
            m.put("specialization", a.getDoctor().getSpecialization());
            m.put("appointmentDate", a.getAppointmentDate());
            m.put("timeSlot", a.getTimeSlot());
            m.put("symptoms", a.getSymptoms());
            m.put("status", a.getStatus());
            m.put("notes", a.getNotes());
            result.add(m);
        }
        return result;
    }
}
