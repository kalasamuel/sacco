package com.kimwanyi.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.kimwanyi.model.AuditLog;
import com.kimwanyi.model.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogDAO extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);

    List<AuditLog> findByUserAccountIdOrderByCreatedAtDesc(Long userAccountId);

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime from, LocalDateTime to);
}
