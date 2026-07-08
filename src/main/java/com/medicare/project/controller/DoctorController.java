package com.medicare.project.controller;

import com.medicare.project.entity.*;
import com.medicare.project.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired private DoctorRepository doctorRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(toDtoList(doctorRepository.findAll()));
    }

    @GetMapping("/available")
    public ResponseEntity<?> getAvailable() {
        return ResponseEntity.ok(toDtoList(doctorRepository.findByStatus("AVAILABLE")));
    }

    @GetMapping("/specialization/{spec}")
    public ResponseEntity<?> getBySpecialization(@PathVariable String spec) {
        return ResponseEntity.ok(toDtoList(doctorRepository.findBySpecialization(spec)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Doctor doc = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        return ResponseEntity.ok(toDto(doc));
    }

    // Admin adds a new doctor
    @PostMapping
    public ResponseEntity<?> addDoctor(@RequestBody Map<String, String> body) {
        String name     = body.getOrDefault("name", "").trim();
        String spec     = body.getOrDefault("specialization", "").trim();
        String qual     = body.getOrDefault("qualification", "");
        String exp      = body.getOrDefault("experience", "");
        String phone    = body.getOrDefault("phone", "");
        String email    = body.getOrDefault("email", "");
        String days     = body.getOrDefault("availableDays", "Mon-Sat");
        String time     = body.getOrDefault("availableTime", "09:00-17:00");
        String feeStr   = body.getOrDefault("consultationFee", "0");

        if (name.isEmpty() || spec.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name and specialization are required."));
        }

        // Auto-generate username from doctor name
        String baseUsername = "dr." + name.toLowerCase().replaceAll("\\s+", ".").replaceAll("[^a-z0-9.]", "");
        String username = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + suffix++;
        }

        // Generate temporary password
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        String tempPassword = "Doc@" + sb;

        Role doctorRole = roleRepository.findByName("ROLE_DOCTOR")
                .orElseThrow(() -> new RuntimeException("ROLE_DOCTOR not found"));

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus("APPROVED");
        user.setRoles(Set.of(doctorRole));
        userRepository.save(user);

        Double fee = 0.0;
        try { fee = Double.parseDouble(feeStr); } catch (Exception ignored) {}

        Doctor doctor = new Doctor();
        doctor.setName(name);
        doctor.setSpecialization(spec);
        doctor.setQualification(qual);
        doctor.setExperience(exp);
        doctor.setPhone(phone);
        doctor.setEmail(email);
        doctor.setAvailableDays(days);
        doctor.setAvailableTime(time);
        doctor.setConsultationFee(fee);
        doctor.setStatus("AVAILABLE");
        doctor.setUser(user);
        doctorRepository.save(doctor);

        return ResponseEntity.ok(Map.of(
            "message", "Doctor " + name + " added successfully.",
            "loginUsername", username,
            "loginPassword", tempPassword
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctor(@PathVariable Long id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        Long userId = doctor.getUser() != null ? doctor.getUser().getId() : null;
        // Remove doctor (appointments keep foreign key but doctor is gone — use delete carefully)
        List<Appointment> appts = appointmentRepository.findByDoctorId(id);
        appointmentRepository.deleteAll(appts);
        doctorRepository.delete(doctor);
        if (userId != null) {
            try { userRepository.deleteById(userId); } catch (Exception ignored) {}
        }
        return ResponseEntity.ok(Map.of("message", "Doctor deleted."));
    }

    // Doctor: view their own appointments
    @GetMapping("/my/appointments")
    public ResponseEntity<?> myAppointments(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Doctor profile not found"));
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdOrderByAppointmentDateDesc(doctor.getId());
        return ResponseEntity.ok(appointmentsToDto(appointments));
    }

    private List<Map<String, Object>> toDtoList(List<Doctor> doctors) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Doctor d : doctors) result.add(toDto(d));
        return result;
    }

    private Map<String, Object> toDto(Doctor d) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("specialization", d.getSpecialization());
        m.put("qualification", d.getQualification());
        m.put("experience", d.getExperience());
        m.put("phone", d.getPhone());
        m.put("email", d.getEmail());
        m.put("availableDays", d.getAvailableDays());
        m.put("availableTime", d.getAvailableTime());
        m.put("consultationFee", d.getConsultationFee());
        m.put("status", d.getStatus());
        return m;
    }

    private List<Map<String, Object>> appointmentsToDto(List<Appointment> appointments) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : appointments) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("patientName", a.getPatient().getName());
            m.put("patientId", a.getPatient().getId());
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
