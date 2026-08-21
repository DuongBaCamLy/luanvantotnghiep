package com.scse.curriculum.notification.service;

import com.scse.curriculum.notification.entity.Notification;
import com.scse.curriculum.notification.repository.NotificationRepository;
import com.scse.curriculum.user.entity.UserAccount;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserAccountRepository userRepository;

    @Transactional
    public Notification createNotification(Integer userId, String title, String message, String type) {
        UserAccount user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();

        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }
}
