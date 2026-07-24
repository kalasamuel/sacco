package com.pahappa.sacco.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.function.Function;

/**
 * Defines the transaction boundary for a unit of work against the
 * request-bound EntityManager (see {@link EntityManagerProvider}).
 *
 * This is where the service layer will wrap every
 * money-moving operation — e.g. "lock the account row, insert a
 * Transaction row, update the balance" all happen inside ONE call to
 * #execute, so they either all commit together or all roll back
 * together. Without this, a crash between the balance update and the
 * ledger insert could leave the two permanently inconsistent — exactly
//  * the "ledger and passbook disagree" failure mode, just
//  * moved into the database instead of on paper.
 */
public final class TransactionUtil {

    private TransactionUtil() {
    }

    public static <T> T execute(Function<EntityManager, T> work) {
        EntityManager em = EntityManagerProvider.get();
        EntityTransaction tx = em.getTransaction();
        boolean startedHere = !tx.isActive();
        if (startedHere) {
            tx.begin();
        }
        try {
            T result = work.apply(em);
            if (startedHere) {
                tx.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (startedHere && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public static void executeVoid(java.util.function.Consumer<EntityManager> work) {
        execute(em -> {
            work.accept(em);
            return null;
        });
    }
}
