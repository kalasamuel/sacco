package com.pahappa.sacco.dao;

import com.pahappa.sacco.entity.User;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.Optional;

@ApplicationScoped
public class UserDao extends GenericDao<User, Long> {

    public UserDao() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        try {
            TypedQuery<User> query = em().createQuery(
                    "SELECT u FROM User u LEFT JOIN FETCH u.memberProfile WHERE u.username = :username", User.class);
            query.setParameter("username", username == null ? "" : username.trim());
            return Optional.of(query.getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
