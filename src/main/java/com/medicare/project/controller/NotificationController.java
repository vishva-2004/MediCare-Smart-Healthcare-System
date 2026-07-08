package com.medicare.project.controller;

import com.medicare.project.entity.Notification;
import com.medicare.project.entity.User;
import com.medicare.project.repository.NotificationRepository;
import com.medicare.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long unread = notificationRepository.countUnreadByUserId(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifications) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("message", n.getMessage());
            m.put("read", n.isSeen());
            m.put("createdAt", n.getCreatedAt());
            result.add(m);
        }
        return ResponseEntity.ok(Map.of("notifications", result, "unread", unread));
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<?> markRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setSeen(true);
            notificationRepository.save(n);
        });
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(Authentication auth) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Notification> unread = notificationRepository.findUnreadByUserId(user.getId());
        unread.forEach(n -> n.setSeen(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }
}
