package lavi.minecraft.diagnostics.session.admission;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSessionAdmissionRaceTest {
    @Test
    void simultaneousOrdinaryRejectionsClaimOnlyOneCanonicalCapToken() throws Exception {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("ordinary-race-session");
        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            assertTrue(authority.admit(
                    DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        }

        int contenders = 32;
        ExecutorService executor = Executors.newFixedThreadPool(contenders);
        CountDownLatch ready = new CountDownLatch(contenders);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<DiagnosticAdmissionDecision>> futures = new ArrayList<>();
        try {
            for (int index = 0; index < contenders; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return authority.admit(
                            DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL));
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int capTokens = 0;
            for (Future<DiagnosticAdmissionDecision> future : futures) {
                if (future.get(10, TimeUnit.SECONDS).canonicalCapToken() != null) {
                    capTokens++;
                }
            }
            assertEquals(1, capTokens);
            assertEquals(1, authority.snapshot()
                    .family(DiagnosticEventFamily.CANONICAL_CAP).admittedRequests());
            assertEquals(4_937, authority.snapshot().admittedSlots());
            assertEquals(contenders, authority.snapshot()
                    .family(DiagnosticEventFamily.ORDINARY_DETAIL).suppressedRequests());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void simultaneousOrdinaryAndCriticalHardBoundaryTriggersStillProduceOneCapToken() throws Exception {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("mixed-trigger-race-session");
        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            assertTrue(authority.admit(
                    DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        }
        admitTimes(authority, DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL, 4);
        admitTimes(authority, DiagnosticEventFamily.ROUTINE_STORE_TERMINAL, 2);
        admitTimes(authority, DiagnosticEventFamily.EXCEPTION_COVERAGE, 16);
        admitTimes(authority, DiagnosticEventFamily.AGGREGATE_CHECKPOINT, 8);
        admitTimes(authority, DiagnosticEventFamily.NON_STORE_TERMINAL, 8);
        admitTimes(authority, DiagnosticEventFamily.SUPPRESSION_CONTROL, 6);
        assertEquals(4_998, authority.snapshot().admittedSlots());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<DiagnosticAdmissionDecision> ordinary = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return authority.admit(
                        DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.ORDINARY_DETAIL));
            });
            Future<DiagnosticAdmissionDecision> critical = executor.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return authority.admit(
                        DiagnosticAdmissionRequest.eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE));
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            int capTokens = (ordinary.get(10, TimeUnit.SECONDS).canonicalCapToken() == null ? 0 : 1)
                    + (critical.get(10, TimeUnit.SECONDS).canonicalCapToken() == null ? 0 : 1);
            assertEquals(1, capTokens);
            assertEquals(1, authority.snapshot()
                    .family(DiagnosticEventFamily.CANONICAL_CAP).admittedRequests());
            assertEquals(4_999, authority.snapshot().admittedSlots());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static void admitTimes(DiagnosticSessionAdmissionAuthority authority,
                                   DiagnosticEventFamily family,
                                   int count) {
        for (int index = 0; index < count; index++) {
            assertTrue(authority.admit(DiagnosticAdmissionRequest.eligible(family)).admitted());
        }
    }
}
