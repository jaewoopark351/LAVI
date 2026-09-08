package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Characterize immutable snapshots under concurrent calls to the synchronized facade.
class StoreDepositAutomaticLifecycleLedgerConcurrencyTest {
    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void concurrentTerminalRecordsProduceAtomicImmutableSnapshots() throws Exception {
        int recordCount = 64;
        StoreDepositAutomaticLifecycleLedger ledger = new StoreDepositAutomaticLifecycleLedger();
        Task maintenance = new TestTask("concurrent-maintenance");
        StoreDepositAutomaticContext context = ledger.beginRun(maintenance, null, 701L);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<StoreDepositAutomaticLifecycleLedger.Snapshot>> calls = new ArrayList<>();
            for (int index = 0; index < recordCount; index++) {
                int identity = index;
                calls.add(() -> {
                    start.await();
                    String transferId = "concurrent-transfer-" + identity;
                    ledger.expectTerminalScopeIdentity(context, "TRANSFER", transferId);
                    return ledger.recordScope(
                            context,
                            "TRANSFER",
                            transferId,
                            "CONCURRENT_CLOSE"
                    ).snapshot();
                });
            }

            List<Future<StoreDepositAutomaticLifecycleLedger.Snapshot>> futures =
                    submitAll(executor, calls, start);
            for (Future<StoreDepositAutomaticLifecycleLedger.Snapshot> future : futures) {
                StoreDepositAutomaticLifecycleLedger.Snapshot snapshot = future.get(10, TimeUnit.SECONDS);
                assertEquals(
                        snapshot.terminalCount("TRANSFER"),
                        snapshot.observedTerminalIdentities().size()
                );
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        StoreDepositAutomaticLifecycleLedger.Snapshot finalSnapshot = ledger.snapshotFor(maintenance);
        assertEquals(recordCount, finalSnapshot.expectedTerminalCount("TRANSFER"));
        assertEquals(recordCount, finalSnapshot.terminalCount("TRANSFER"));
        assertEquals(recordCount, finalSnapshot.expectedTerminalIdentities().size());
        assertEquals(recordCount, finalSnapshot.observedTerminalIdentities().size());
        assertTrue(finalSnapshot.terminalIdentityCoverageComplete());
    }

    private static <T> List<Future<T>> submitAll(ExecutorService executor,
                                                  List<Callable<T>> calls,
                                                  CountDownLatch start) {
        List<Future<T>> futures = calls.stream().map(executor::submit).toList();
        start.countDown();
        return futures;
    }
}
