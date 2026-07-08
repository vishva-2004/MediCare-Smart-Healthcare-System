package com.medicare.project.repository;

import com.medicare.project.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findBySpecialization(String specialization);
    List<Doctor> findByStatus(String status);
    Optional<Doctor> findByUserId(Long userId);
}
