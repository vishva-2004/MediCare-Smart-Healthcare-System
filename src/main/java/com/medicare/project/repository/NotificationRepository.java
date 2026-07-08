package com.medicare.project.repository;

import com.medicare.project.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.seen = false")
    List<Notification> findUnreadByUserId(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.seen = false")
    long countUnreadByUserId(Long userId);
}
