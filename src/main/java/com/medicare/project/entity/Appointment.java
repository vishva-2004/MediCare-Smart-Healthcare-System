package com.medicare.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    private String timeSlot;
    private String symptoms;
    private String notes;

    // PENDING, CONFIRMED, COMPLETED, CANCELLED
    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDate createdDate = LocalDate.now();
}
