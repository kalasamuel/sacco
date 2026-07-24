package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.Member;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class MemberDao extends GenericDao<Member, Long> {

    public MemberDao() {
        super(Member.class);
    }

    public Optional<Member> findByNationalId(String nationalId) {
        try {
            TypedQuery<Member> query = em().createQuery(
                    "SELECT m FROM Member m WHERE m.nationalId = :nationalId", Member.class);
            query.setParameter("nationalId", nationalId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Member> findByMembershipNumber(String membershipNumber) {
        try {
            TypedQuery<Member> query = em().createQuery(
                    "SELECT m FROM Member m WHERE m.membershipNumber = :num", Member.class);
            query.setParameter("num", membershipNumber);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<Member> searchByName(String partialName) {
        TypedQuery<Member> query = em().createQuery(
                "SELECT m FROM Member m WHERE " +
                "LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(:pattern) " +
                "ORDER BY m.lastName, m.firstName", Member.class);
        query.setParameter("pattern", "%" + partialName + "%");
        return query.getResultList();
    }

    public List<Member> findAllActive() {
        return em().createQuery(
                "SELECT m FROM Member m WHERE m.active = TRUE ORDER BY m.lastName, m.firstName",
                Member.class).getResultList();
    }

    public long countActive() {
        return em().createQuery("SELECT COUNT(m) FROM Member m WHERE m.active = TRUE", Long.class)
                .getSingleResult();
    }
}
