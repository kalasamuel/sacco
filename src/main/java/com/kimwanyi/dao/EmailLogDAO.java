package com.kimwanyi.dao;

import com.kimwanyi.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLogDAO extends JpaRepository<EmailLog, Long> {
}
