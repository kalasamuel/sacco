package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.Account;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Optional;

@ApplicationScoped
public class AccountDao extends GenericDao<Account, Long> {

    public AccountDao() {
        super(Account.class);
    }

    public Optional<Account> findByMemberId(Long memberId) {
        try {
            TypedQuery<Account> query = em().createQuery(
                    "SELECT a FROM Account a WHERE a.member.id = :memberId", Account.class);
            query.setParameter("memberId", memberId);
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<Account> findByMemberIdForUpdate(Long memberId) {
        Optional<Account> unlocked = findByMemberId(memberId);
        return unlocked.flatMap(acc -> findByIdForUpdate(acc.getId()));
    }
}
