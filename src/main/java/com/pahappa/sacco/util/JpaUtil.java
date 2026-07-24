package com.pahappa.sacco.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Owns the single, application-wide  EntityManagerFactory.
 * EntityManagerFactory creation is expensive (parses persistence.xml,
 * builds the Hibernate SessionFactory) so it's built exactly once at
 * startup and closed exactly once at shutdown
 */
public final class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "kimwanyiSaccoPU";
    private static volatile EntityManagerFactory entityManagerFactory;

    private JpaUtil() {
    }

    public static synchronized void init() {
        if (entityManagerFactory == null) {
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
    }

    public static EntityManager createEntityManager() {
        if (entityManagerFactory == null) {
            init();
        }
        return entityManagerFactory.createEntityManager();
    }

    public static synchronized void shutdown() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
            entityManagerFactory = null;
        }
    }
}
