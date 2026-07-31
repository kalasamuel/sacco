package com.kimwanyi.service.interfaces;
import java.util.List;

import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.NotificationDto;
import com.kimwanyi.model.enums.NotificationType;

public interface NotificationService {

    void notify(UserAccount recipient, NotificationType type, String title, String message);

    void notifyAdmins(NotificationType type, String title, String message);

    List<NotificationDto> getRecent(Long userAccountId);

    long getUnreadCount(Long userAccountId);

    void markAsRead(Long notificationId);

    void markAllAsRead(Long userAccountId);
}
