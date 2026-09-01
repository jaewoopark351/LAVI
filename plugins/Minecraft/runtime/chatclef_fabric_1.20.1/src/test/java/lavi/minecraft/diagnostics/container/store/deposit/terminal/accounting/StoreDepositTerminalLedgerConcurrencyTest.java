package lavi.minecraft.diagnostics.container.store.deposit.terminal.accounting;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

//20260831_kpopmodder: Prove one terminal token advances each ledger stage exactly once under races.
class StoreDepositTerminalLedgerConcurrencyTest {
    @Test
    void sameTokenConcurrentTransitionsMutateEachStageExactlyOnce() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
            StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                    "same-token-race",
                    StoreDepositTerminalClassification.NATURAL_FINISH,
                    StoreDepositTerminalScope.MANUAL,
                    true,
                    1L
            );

            assertEquals(1, concurrentTrueCount(32, () -> ledger.recordAdmissionGranted(token)));
            StoreDepositTerminalLedgerSnapshot admitted = ledger.snapshot();
            assertEquals(1, admitted.totalTerminalObserved());
            assertEquals(0, admitted.fullTerminalGroupAdmissionPending());
            assertEquals(1, admitted.fullTerminalGroupAdmissionGranted());
            assertEquals(1, admitted.fullTerminalGroupEmissionPending());
            assertEquals(1, admitted.activeAccountingTokenCount());
            assertEquals(2, admitted.ledgerRevision());

            assertEquals(1, concurrentTrueCount(32, () -> ledger.recordEmissionCompleted(token)));
            StoreDepositTerminalLedgerSnapshot completed = ledger.snapshot();
            assertEquals(0, completed.fullTerminalGroupEmissionPending());
            assertEquals(1, completed.fullTerminalGroupEmissionCompleted());
            assertEquals(0, completed.fullTerminalGroupEmissionFailedAfterAdmission());
            assertEquals(0, completed.activeAccountingTokenCount());
            assertEquals(3, completed.ledgerRevision());
            assertTrue(completed.settled());
            assertTrue(completed.accountingEquationsHold());
            assertEquals(StoreDepositTerminalReconciliationStatus.EXACT,
                    completed.reconciliationStatus());
        });
    }

    @Test
    void competingSameTokenFinalOutcomesConsumeThePendingEmissionExactlyOnce() {
        assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
            StoreDepositTerminalLedger ledger = new StoreDepositTerminalLedger();
            StoreDepositTerminalAccountingToken token = ledger.recordAuthoritativeTerminal(
                    "competing-final-outcomes",
                    StoreDepositTerminalClassification.UNKNOWN_STOP,
                    StoreDepositTerminalScope.MANUAL,
                    true,
                    2L
            );
            assertTrue(ledger.recordAdmissionGranted(token));
            List<Callable<Boolean>> competingOutcomes = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                competingOutcomes.add((index & 1) == 0
                        ? () -> ledger.recordEmissionCompleted(token)
                        : () -> ledger.recordEmissionFailedAfterAdmission(token));
            }

            assertEquals(1, concurrentTrueCount(competingOutcomes));

            StoreDepositTerminalLedgerSnapshot snapshot = ledger.snapshot();
            long completed = snapshot.fullTerminalGroupEmissionCompleted();
            long failed = snapshot.fullTerminalGroupEmissionFailedAfterAdmission();
            assertEquals(1L, completed + failed);
            assertEquals(0, snapshot.fullTerminalGroupEmissionPending());
            assertEquals(0, snapshot.activeAccountingTokenCount());
            assertEquals(failed, snapshot.ledgerCoverageGapCount());
            assertEquals(3, snapshot.ledgerRevision());
            assertTrue(snapshot.settled());
            assertTrue(snapshot.accountingEquationsHold());
            assertFalse(snapshot.counterSaturated());
        });
    }

    private static int concurrentTrueCount(int threadCount, Callable<Boolean> action)
            throws Exception {
        List<Callable<Boolean>> actions = new ArrayList<>();
        for (int index = 0; index < threadCount; index++) {
            actions.add(action);
        }
        return concurrentTrueCount(actions);
    }

    private static int concurrentTrueCount(List<Callable<Boolean>> actions)
            throws Exception {
        int threadCount = actions.size();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (Callable<Boolean> action : actions) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(5L, TimeUnit.SECONDS));
                    return action.call();
                }));
            }
            assertTrue(ready.await(5L, TimeUnit.SECONDS));
            start.countDown();
            int trueCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5L, TimeUnit.SECONDS)) {
                    trueCount++;
                }
            }
            return trueCount;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
