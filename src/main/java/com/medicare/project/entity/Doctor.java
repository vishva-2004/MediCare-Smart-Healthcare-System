package com.medicare.project.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String specialization;

    private String qualification;
    private String experience;
    private String phone;
    private String email;
    private String availableDays;
    private String availableTime;
    private Double consultationFee;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
