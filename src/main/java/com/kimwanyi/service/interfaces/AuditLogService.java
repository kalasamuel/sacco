package com.kimwanyi.service.interfaces;

import java.util.List;

import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.AuditLogDto;
import com.kimwanyi.model.dto.forms.AuditLogFilterForm;
import com.kimwanyi.model.enums.AuditAction;

public interface AuditLogService {

    void record(UserAccount actor, AuditAction action, String entityType, Long entityId, String description);

    List<AuditLogDto> search(AuditLogFilterForm filter);

    List<AuditLogDto> findRecent(int limit);
}
