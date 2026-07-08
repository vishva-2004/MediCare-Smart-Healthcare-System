package com.medicare.project.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_seen", nullable = false)
    private boolean seen = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
