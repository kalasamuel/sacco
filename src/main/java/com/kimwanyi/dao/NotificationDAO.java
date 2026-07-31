package com.kimwanyi.dao;

import java.util.List;
import com.kimwanyi.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDAO extends JpaRepository<Notification, Long> {

    List<Notification> findTop15ByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);

    long countByUserAccountIdAndReadStatusFalse(Long userAccountId);

    @Modifying
    @Query("update Notification n set n.readStatus = true, n.readAt = CURRENT_TIMESTAMP "
            + "where n.userAccount.id = :userAccountId and n.readStatus = false")
    void markAllAsRead(@Param("userAccountId") Long userAccountId);
}
