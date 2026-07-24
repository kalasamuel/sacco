package com.pahappa.sacco.util;

import javax.persistence.EntityManager;

public final class EntityManagerProvider {

    private static final ThreadLocal<EntityManager> CURRENT = new ThreadLocal<>();

    private EntityManagerProvider() {
    }

    public static void bind(EntityManager em) {
        CURRENT.set(em);
    }

    public static EntityManager get() {
        EntityManager em = CURRENT.get();
        if (em == null) {
            throw new IllegalStateException(
                    "No EntityManager bound to this thread. Is PersistenceFilter mapped to this request path?");
        }
        return em;
    }

    public static void unbind() {
        CURRENT.remove();
    }
}
