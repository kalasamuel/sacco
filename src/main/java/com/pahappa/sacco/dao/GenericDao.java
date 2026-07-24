package com.pahappa.sacco.dao;

import com.pahappa.sacco.util.EntityManagerProvider;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public abstract class GenericDao<T, ID> {

    private final Class<T> entityClass;

    protected GenericDao(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected EntityManager em() {
        return EntityManagerProvider.get();
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(em().find(entityClass, id));
    }

    public Optional<T> findByIdForUpdate(ID id) {
        return Optional.ofNullable(em().find(entityClass, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public List<T> findAll() {
        return em().createQuery("SELECT e FROM " + entityClass.getSimpleName() + " e", entityClass)
                .getResultList();
    }

    public T save(T entity) {
        em().persist(entity);
        return entity;
    }

    public T update(T entity) {
        return em().merge(entity);
    }
}
