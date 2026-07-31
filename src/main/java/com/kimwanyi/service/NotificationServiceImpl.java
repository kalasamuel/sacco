package com.kimwanyi.service;

import java.util.List;
import com.kimwanyi.exception.ResourceNotFoundException;
import com.kimwanyi.util.converter.NotificationConverter;
import com.kimwanyi.dao.NotificationDAO;
import com.kimwanyi.dao.UserAccountDAO;
import com.kimwanyi.model.Notification;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.NotificationDto;
import com.kimwanyi.model.enums.NotificationType;
import com.kimwanyi.model.enums.Role;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.kimwanyi.service.interfaces.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;
    private final UserAccountDAO userAccountDAO;
    private final NotificationConverter notificationConverter;

    public NotificationServiceImpl(
            NotificationDAO notificationDAO,
            UserAccountDAO userAccountDAO,
            NotificationConverter notificationConverter
    ) {
        this.notificationDAO = notificationDAO;
        this.userAccountDAO = userAccountDAO;
        this.notificationConverter = notificationConverter;
    }

    @Override
    @Transactional
    public void notify(UserAccount recipient, NotificationType type, String title, String message) {
        Notification notification = new Notification();
        notification.setUserAccount(recipient);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notificationDAO.save(notification);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyAdmins(NotificationType type, String title, String message) {
        List<UserAccount> admins = userAccountDAO.findByRole(Role.ADMIN);
        for (UserAccount admin : admins) {
            notify(admin, type, title, message);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getRecent(Long userAccountId) {
        return notificationDAO.findTop15ByUserAccountIdOrderByCreatedAtDesc(userAccountId).stream()
                .map(notificationConverter::toDto)
                .toList();
    }

    @Override
    public long getUnreadCount(Long userAccountId) {
        return notificationDAO.countByUserAccountIdAndReadStatusFalse(userAccountId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationDAO.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + notificationId));
        notification.setReadStatus(true);
        notificationDAO.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userAccountId) {
        notificationDAO.markAllAsRead(userAccountId);
    }
}
