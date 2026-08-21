package com.scse.curriculum.config;

import com.scse.curriculum.notification.repository.NotificationRepository;
import com.scse.curriculum.user.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("accessGuard")
@RequiredArgsConstructor
public class AccessGuard {

    private final UserAccountRepository userAccountRepository;
    private final NotificationRepository notificationRepository;

    public boolean isCurrentUser(Integer userId, Authentication authentication) {
        Integer currentUserId = currentUserId(authentication);
        return currentUserId != null && userId != null && currentUserId.equals(userId);
    }


    public boolean ownsNotification(Integer notificationId, Authentication authentication) {
        Integer currentUserId = currentUserId(authentication);
        if (currentUserId == null || notificationId == null) {
            return false;
        }

        return notificationRepository.findById(notificationId)
                .map(notification -> notification.getUser() != null
                        && currentUserId.equals(notification.getUser().getId()))
                .orElse(false);
    }

    private Integer currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return null;
        }

        return userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(authentication.getName(), authentication.getName())
                .map(user -> user.getId())
                .orElse(null);
    }
}
