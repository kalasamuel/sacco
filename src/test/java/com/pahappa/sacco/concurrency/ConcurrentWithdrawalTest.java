package com.pahappa.sacco.concurrency;

import com.pahappa.sacco.entity.Account;
import com.pahappa.sacco.entity.Member;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Persistence;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single most important test in this project — it directly verifies
 * the concurrency guarantee decided in Section 1 and implemented in
 * Section 6 (AccountDao.findByIdForUpdate / findByMemberIdForUpdate).
 *
 * Scenario: an account with exactly enough balance for ONE of two
 * concurrent withdrawal requests, but not both. Without pessimistic
 * locking, both threads could read the same starting balance, both pass
 * the "sufficient funds" check, and both commit — overdrawing the
 * account. This is the literal failure mode from the brief: "a member
 * withdraws more than they have because the cashier misread the
 * balance," just happening between two threads instead of two cashiers.
 *
 * The test asserts that exactly one withdrawal succeeds, the other
 * correctly fails with insufficient funds, and the final balance is
 * never negative and never below the minimum — proving the lock, not
 * luck, is what prevents the race.
 *
 * NOTE: this runs against H2 (see src/test/resources/META-INF/persistence.xml),
 * not the production MySQL database. H2's row-locking behavior under
 * PESSIMISTIC_WRITE is close enough to MySQL/InnoDB's to validate the
 * locking PATTERN correctly, but is not a substitute for also observing
 * this behavior against real MySQL before final submission — different
 * database engines can have subtly different lock wait/timeout semantics.
 */
class ConcurrentWithdrawalTest {

    private static EntityManagerFactory emf;
    private Long memberId;
    private Long accountId;

    @BeforeAll
    static void setUpFactory() {
        emf = Persistence.createEntityManagerFactory("kimwanyiSaccoTestPU");
    }

    @AfterAll
    static void tearDownFactory() {
        if (emf != null) {
            emf.close();
        }
    }

    @AfterEach
    void cleanUp() {
        // create-drop recreates schema per EMF, not per test — clear rows
        // between tests to keep each test's starting state independent.
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Account").executeUpdate();
        em.createQuery("DELETE FROM Member").executeUpdate();
        em.getTransaction().commit();
        em.close();
    }

    private void seedAccountWithBalance(BigDecimal balance) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        Member member = new Member("MBR-CONC-1", "NIN-CONC-1", "Concurrent", "Tester", null, null);
        em.persist(member);
        Account account = new Account(member);
        account.credit(balance);
        em.persist(account);
        em.getTransaction().commit();
        this.memberId = member.getId();
        this.accountId = account.getId();
        em.close();
    }

    /**
     * Two threads attempt to withdraw 30,000 each from an account that
     * starts with a balance high enough for only ONE such withdrawal to
     * succeed without breaching the minimum balance floor. Both threads
     * are synchronized to start at (as close as possible to) the same
     * instant via a CountDownLatch, to maximize the chance of exposing a
     * race condition if the locking were absent or broken.
     */
    @Test
    void onlyOneOfTwoConcurrentWithdrawalsSucceedsWhenFundsAllowOnlyOne() throws InterruptedException {
        // Minimum balance is 20,000 (Account default). Seed 70,000 so
        // exactly one 30,000 withdrawal leaves 40,000 (fine), but a
        // second would leave 10,000 (violates the minimum) — this
        // account can service exactly one of the two concurrent requests.
        seedAccountWithBalance(new BigDecimal("70000.00"));

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable withdrawTask = () -> {
            readyLatch.countDown();
            try {
                startLatch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            EntityManager em = emf.createEntityManager();
            try {
                em.getTransaction().begin();
                // The line under test: PESSIMISTIC_WRITE lock acquisition,
                // exactly as AccountDao.findByIdForUpdate does in production.
                Account account = em.find(Account.class, accountId, LockModeType.PESSIMISTIC_WRITE);
                account.debit(new BigDecimal("30000.00"));
                em.getTransaction().commit();
                successCount.incrementAndGet();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                failureCount.incrementAndGet();
            } finally {
                em.close();
            }
        };

        pool.submit(withdrawTask);
        pool.submit(withdrawTask);

        readyLatch.await(5, TimeUnit.SECONDS); // both threads ready at the gate
        startLatch.countDown();                 // release both as close to simultaneously as possible

        pool.shutdown();
        assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS), "Withdrawal threads did not complete in time.");

        // The core assertion: exactly one withdrawal succeeded, exactly
        // one correctly failed — never both succeeding (which would mean
        // the lock did nothing) and never both failing (which would mean
        // the lock over-serialized incorrectly).
        assertEquals(1, successCount.get(), "Expected exactly one withdrawal to succeed.");
        assertEquals(1, failureCount.get(), "Expected exactly one withdrawal to be rejected.");

        EntityManager verifyEm = emf.createEntityManager();
        Account finalAccount = verifyEm.find(Account.class, accountId);
        assertEquals(new BigDecimal("40000.00"), finalAccount.getBalance(),
                "Final balance must reflect exactly one successful 30,000 withdrawal from 70,000.");
        assertTrue(finalAccount.getBalance().compareTo(finalAccount.getMinBalance()) >= 0,
                "Final balance must never be below the minimum balance floor.");
        verifyEm.close();
    }
}
