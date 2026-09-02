package lavi.minecraft.diagnostics.container.store.deposit.route;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.candidate.StoreContainerRouteCheckpoint;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize checkpoint emission, field order, and post-emission acknowledgement.
class StoreDepositCheckpointDiagnosticsTest {
    @BeforeEach
    void enableFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
        ChatClefDiagnostics.setBoundaryEnabled(true);
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void emitsChangedCheckpointOnceAndAcknowledgesItsRangeAggregate() {
        long startTick = ChatClefDiagnostics.currentClientTickId();
        StoreDepositOperationState state = new StoreDepositOperationState(
                new StoreDepositOperationContext(
                        "operation-a",
                        "BARE_DEPOSIT_ALL_COMMAND",
                        null,
                        startTick,
                        0
                )
        );
        BlockPos raw = new BlockPos(50, 64, 0);
        recordRangeDecision(state, raw, false, new Vec3d(0, 64, 0));
        recordRangeDecision(state, raw, true, new Vec3d(1, 64, 0));
        for (int tick = 0; tick < 1200; tick++) {
            ChatClefDiagnostics.onClientTickHead();
        }

        StoreDepositCheckpointDiagnostics diagnostics =
                new StoreDepositCheckpointDiagnostics(new StoreDepositEmissionGate());
        String output = captureOutput(() -> diagnostics.emitIfDue(null, state));

        assertEquals(1, occurrences(output, "event=STORE_DEPOSIT_CHECKPOINT_SUMMARY"));
        String line = output.lines()
                .filter(value -> value.contains("event=STORE_DEPOSIT_CHECKPOINT_SUMMARY"))
                .findFirst()
                .orElseThrow();
        assertOrdered(
                line,
                "gameTick=" + (startTick + 1200),
                "storeOperationId=operation-a",
                "checkpointSequence=1",
                "elapsedTicks=1200",
                "sinceLastCheckpointRaw50CrossingCount=1",
                "storeBudgetOperationDetailEmittedCount=1"
        );

        assertEquals("", captureOutput(() -> diagnostics.emitIfDue(null, state)));

        recordRangeDecision(state, raw, false, new Vec3d(0, 64, 0));
        StoreContainerRouteCheckpoint next = state.routeState()
                .checkpoint(startTick + 2400)
                .orElseThrow();
        assertEquals(1, next.rangeAggregateSinceLastCheckpoint().raw50CrossingCount());
    }

    private static void recordRangeDecision(StoreDepositOperationState state,
                                            BlockPos raw,
                                            boolean withinRange,
                                            Vec3d playerPosition) {
        state.routeState().recordParentDecision(
                "OPEN_EXISTING",
                true,
                raw,
                true,
                withinRange,
                false,
                false,
                null,
                withinRange ? "RAW_CLOSEST_WITHIN_50" : "RAW_PRESENT_BUT_OUTSIDE_RANGES",
                "targets-a",
                playerPosition
        );
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static void assertOrdered(String text, String... tokens) {
        int previous = -1;
        for (String token : tokens) {
            int current = text.indexOf(token, previous + 1);
            assertTrue(
                    current > previous,
                    () -> "Missing or out of order: " + token + " in: " + text
            );
            previous = current;
        }
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int offset = 0;
        while ((offset = text.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }
}
