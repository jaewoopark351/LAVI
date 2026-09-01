package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleRegistry;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticSessionRuntime;
import lavi.minecraft.diagnostics.toolselect.lifecycle.ToolSelectionDiagnosticStateObserver;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionDiagnosticShaper;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionShapingDecision;
import lavi.minecraft.diagnostics.toolselect.support.DiagnosticDeduplicator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectionStrictOffContractTest {
    @Test
    void offWaitsForTheEligibilityLeaseThenClearsEveryToolSelectionState()
            throws Exception {
        DiagnosticModeController mode =
                new DiagnosticModeController(DiagnosticOutputMode.BOUNDARY);
        DiagnosticSessionRuntime runtime = new DiagnosticSessionRuntime(
                mode,
                new DiagnosticSessionAdmissionAuthority("tool-selection-off-test")
        );
        DiagnosticSessionLifecycleRegistry lifecycle =
                new DiagnosticSessionLifecycleRegistry();
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();
        DiagnosticDeduplicator deduplicator = new DiagnosticDeduplicator();
        ToolSavePolicySnapshotEmissionLimiter limiter =
                new ToolSavePolicySnapshotEmissionLimiter();
        AtomicInteger clearCount = new AtomicInteger();
        lifecycle.register(new ToolSelectionDiagnosticStateObserver(() -> {
            assertTrue(mode.isBoundaryEnabled());
            clearCount.incrementAndGet();
            shaper.clearForModeOff();
        }));
        lifecycle.register(new ToolSelectionDiagnosticStateObserver(() -> {
            assertTrue(mode.isBoundaryEnabled());
            clearCount.incrementAndGet();
            deduplicator.clearForModeOff();
        }));
        lifecycle.register(new ToolSelectionDiagnosticStateObserver(() -> {
            assertTrue(mode.isBoundaryEnabled());
            clearCount.incrementAndGet();
            limiter.clearForModeOff();
        }));

        assertTrue(runtime.runIfEligible(() -> {
            assertTrue(shaper.evaluate(
                    true,
                    "selection",
                    "selected=iron_pickaxe",
                    10L
            ).emitEvent());
            assertFalse(shaper.evaluate(
                    true,
                    "selection",
                    "selected=iron_pickaxe",
                    11L
            ).emitEvent());
            assertTrue(deduplicator.shouldEmit("equip-result", "iron"));
            assertFalse(deduplicator.shouldEmit("equip-result", "iron"));
            assertTrue(limiter.evaluate("snapshot", 10L).emitEvent);
            assertFalse(limiter.evaluate("snapshot", 11L).emitEvent);
        }));

        CountDownLatch leaseEntered = new CountDownLatch(1);
        CountDownLatch releaseLease = new CountDownLatch(1);
        CountDownLatch offRequested = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> eligibleWork = executor.submit(() -> runtime.runIfEligible(() -> {
                leaseEntered.countDown();
                await(releaseLease);
            }));
            await(leaseEntered);

            Future<?> offTransition = executor.submit(() -> {
                offRequested.countDown();
                runtime.setBoundaryEnabled(false, lifecycle::notifyBeforeModeOff);
            });
            await(offRequested);

            assertFalse(offTransition.isDone());
            assertEquals(0, clearCount.get());
            assertTrue(mode.isBoundaryEnabled());

            releaseLease.countDown();
            assertTrue(eligibleWork.get(5L, TimeUnit.SECONDS));
            offTransition.get(5L, TimeUnit.SECONDS);

            assertEquals(3, clearCount.get());
            assertTrue(mode.isOff());

            AtomicBoolean offActionRan = new AtomicBoolean();
            assertFalse(runtime.runIfEligible(() -> {
                offActionRan.set(true);
                shaper.evaluate(true, "selection", "selected=diamond_pickaxe", 12L);
                deduplicator.shouldEmit("equip-result", "diamond");
                limiter.evaluate("new-snapshot", 12L);
            }));
            assertFalse(offActionRan.get());

            runtime.setBoundaryEnabled(true);
            assertEquals(3, clearCount.get());
            AtomicReference<ToolSelectionShapingDecision> afterReenable =
                    new AtomicReference<>();
            AtomicBoolean deduplicatorEmitted = new AtomicBoolean();
            AtomicBoolean limiterEmitted = new AtomicBoolean();
            assertTrue(runtime.runIfEligible(() -> {
                afterReenable.set(shaper.evaluate(
                        true,
                        "selection",
                        "selected=iron_pickaxe",
                        13L
                ));
                deduplicatorEmitted.set(
                        deduplicator.shouldEmit("equip-result", "iron")
                );
                limiterEmitted.set(limiter.evaluate("snapshot", 13L).emitEvent);
            }));
            assertNotNull(afterReenable.get());
            assertTrue(afterReenable.get().emitEvent());
            assertTrue(deduplicatorEmitted.get());
            assertTrue(limiterEmitted.get());
        } finally {
            releaseLease.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5L, TimeUnit.SECONDS));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
