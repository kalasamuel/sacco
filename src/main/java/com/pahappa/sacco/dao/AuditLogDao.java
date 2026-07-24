package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.AuditLog;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.TypedQuery;
import java.util.List;

@ApplicationScoped
public class AuditLogDao extends GenericDao<AuditLog, Long> {

    public AuditLogDao() {
        super(AuditLog.class);
    }

    public List<AuditLog> findByEntity(String entityType, Long entityId) {
        TypedQuery<AuditLog> query = em().createQuery(
                "SELECT a FROM AuditLog a WHERE a.entityType = :type AND a.entityId = :id " +
                "ORDER BY a.createdAt DESC", AuditLog.class);
        query.setParameter("type", entityType);
        query.setParameter("id", entityId);
        return query.getResultList();
    }
}
