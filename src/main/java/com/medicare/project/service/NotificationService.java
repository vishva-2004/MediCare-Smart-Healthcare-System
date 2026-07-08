package com.medicare.project.service;

import com.medicare.project.entity.Notification;
import com.medicare.project.entity.User;
import com.medicare.project.repository.NotificationRepository;
import com.medicare.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    public void notifyUser(Long userId, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setMessage(message);
        notificationRepository.save(n);
    }

    public void notifyAllAdmins(String message) {
        List<User> all = userRepository.findAll();
        for (User u : all) {
            boolean isAdmin = u.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("ROLE_ADMIN"));
            if (isAdmin) notifyUser(u.getId(), message);
        }
    }

    public void notifyRole(String roleName, String message) {
        List<User> all = userRepository.findAll();
        for (User u : all) {
            boolean hasRole = u.getRoles().stream()
                    .anyMatch(r -> r.getName().equals(roleName));
            if (hasRole) notifyUser(u.getId(), message);
        }
    }
}
