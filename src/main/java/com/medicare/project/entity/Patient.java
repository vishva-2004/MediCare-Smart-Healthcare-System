package com.medicare.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String phone;
    private String email;
    private String address;
    private String bloodGroup;
    private Integer age;
    private String gender;
    private LocalDate registeredDate = LocalDate.now();

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;
}
