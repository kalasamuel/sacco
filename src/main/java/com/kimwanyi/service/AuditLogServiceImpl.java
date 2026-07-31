package com.kimwanyi.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.kimwanyi.service.interfaces.AuditLogService;
import com.kimwanyi.util.converter.AuditLogConverter;
import com.kimwanyi.dao.AuditLogDAO;
import com.kimwanyi.model.AuditLog;
import com.kimwanyi.model.UserAccount;
import com.kimwanyi.model.dto.AuditLogDto;
import com.kimwanyi.model.dto.forms.AuditLogFilterForm;
import com.kimwanyi.model.enums.AuditAction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogDAO auditLogDAO;
    private final AuditLogConverter auditLogConverter;

    public AuditLogServiceImpl(AuditLogDAO auditLogDAO,
                               AuditLogConverter auditLogConverter) {
        this.auditLogDAO = auditLogDAO;
        this.auditLogConverter = auditLogConverter;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UserAccount actor, AuditAction action,
                       String entityType, Long entityId, String description) {
        AuditLog log = new AuditLog();
        log.setUserAccount(actor);
        log.setAction(action);
        log.setEntityType(entityType != null ? entityType : "");
        log.setEntityId(entityId);
        log.setDescription(description);
        auditLogDAO.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> search(AuditLogFilterForm filter) {
        List<AuditLog> logs = auditLogDAO.findAll(
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));

        return logs.stream()
                .filter(l -> filter.getAction() == null || l.getAction() == filter.getAction())
                .filter(l -> {
                    if (filter.getUsername() == null || filter.getUsername().isBlank()) return true;
                    String kw = filter.getUsername().toLowerCase();
                    return l.getUserAccount() != null
                            && l.getUserAccount().getUsername().toLowerCase().contains(kw);
                })
                .filter(l -> {
                    if (filter.getDateFrom() == null) return true;
                    return l.getCreatedAt() != null
                            && !l.getCreatedAt().toLocalDate().isBefore(filter.getDateFrom());
                })
                .filter(l -> {
                    if (filter.getDateTo() == null) return true;
                    return l.getCreatedAt() != null
                            && !l.getCreatedAt().toLocalDate().isAfter(filter.getDateTo());
                })
                .map(auditLogConverter::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogDto> findRecent(int limit) {
        return auditLogDAO.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .limit(limit)
                .map(auditLogConverter::toDto)
                .collect(Collectors.toList());
    }
}
