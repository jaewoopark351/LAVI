package lavi.minecraft.diagnostics.toolselect.shaping;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolSelectionDiagnosticShaperTest {
    @Test
    void offEvaluationDoesNotMutateChannelOrSuppressionState() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

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

        ToolSelectionShapingDecision decision = shaper.evaluate(
                false,
                "new-channel",
                "selected=diamond_pickaxe",
                500L
        );

        assertTrue(decision.modeOff());
        assertFalse(decision.emitEvent());
        assertFalse(decision.emitSummary());
        assertEquals(1, shaper.channelStateCount());
        assertEquals(1L, shaper.totalSuppressedRepeatCount());
        assertFalse(shaper.hasChannel("new-channel"));
    }

    @Test
    void modeOffClearDropsThePreOffFingerprintAndSuppressionWindow() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        assertTrue(shaper.evaluate(true, "selection", "selected=iron_pickaxe", 10L).emitEvent());
        assertFalse(shaper.evaluate(true, "selection", "selected=iron_pickaxe", 11L).emitEvent());
        assertEquals(1L, shaper.totalSuppressedRepeatCount());

        shaper.clearForModeOff();

        assertEquals(0, shaper.channelStateCount());
        assertEquals(0L, shaper.totalSuppressedRepeatCount());
        assertTrue(shaper.evaluate(
                true,
                "selection",
                "selected=iron_pickaxe",
                12L
        ).emitEvent());
    }

    @Test
    void unchangedDecisionEmitsOneBoundedSummaryAtTwoHundredTicks() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        ToolSelectionShapingDecision first = shaper.evaluate(
                true,
                "selection",
                "selected=iron_pickaxe",
                0L
        );
        assertTrue(first.emitEvent());

        for (long tick = 1L; tick < 200L; tick++) {
            ToolSelectionShapingDecision suppressed = shaper.evaluate(
                    true,
                    "selection",
                    "selected=iron_pickaxe",
                    tick
            );
            assertFalse(suppressed.emitEvent());
            assertFalse(suppressed.emitSummary());
        }

        ToolSelectionShapingDecision summary = shaper.evaluate(
                true,
                "selection",
                "selected=iron_pickaxe",
                200L
        );

        assertFalse(summary.emitEvent());
        assertTrue(summary.emitSummary());
        assertEquals(200L, summary.suppressedRepeatCount());
        assertEquals(1L, summary.firstObservedTick());
        assertEquals(200L, summary.lastObservedTick());
        assertEquals("selected=iron_pickaxe", summary.summaryFingerprint());
        assertEquals(0L, shaper.totalSuppressedRepeatCount());
    }

    @Test
    void meaningfulDecisionTransitionEmitsImmediatelyWithoutAnEarlySummary() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        assertTrue(shaper.evaluate(true, "selection", "selected=stone_pickaxe", 10L).emitEvent());
        assertFalse(shaper.evaluate(true, "selection", "selected=stone_pickaxe", 11L).emitEvent());

        ToolSelectionShapingDecision changed = shaper.evaluate(
                true,
                "selection",
                "selected=iron_pickaxe",
                12L
        );

        assertTrue(changed.emitEvent());
        assertEquals("selected=iron_pickaxe", changed.eventFingerprint());
        assertFalse(changed.emitSummary());
        assertEquals(1L, changed.suppressedRepeatCount());
        assertEquals(11L, changed.firstObservedTick());
        assertEquals(11L, changed.lastObservedTick());
        assertEquals("selected=stone_pickaxe", changed.summaryFingerprint());
    }

    @Test
    void dueSummaryMayShareTheTickWithAnImmediateTransitionEvent() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        assertTrue(shaper.evaluate(true, "selection", "selected=stone_pickaxe", 0L).emitEvent());
        assertFalse(shaper.evaluate(true, "selection", "selected=stone_pickaxe", 100L).emitSummary());

        ToolSelectionShapingDecision changed = shaper.evaluate(
                true,
                "selection",
                "selected=iron_pickaxe",
                200L
        );

        assertTrue(changed.emitEvent());
        assertTrue(changed.emitSummary());
        assertEquals(1L, changed.suppressedRepeatCount());
        assertEquals(100L, changed.firstObservedTick());
        assertEquals(100L, changed.lastObservedTick());
    }

    @Test
    void channelRegistryNeverExceedsTheFixedMaximum() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        for (int index = 0; index <= ToolSelectionDiagnosticShaper.MAX_CHANNEL_STATES; index++) {
            assertTrue(shaper.evaluate(
                    true,
                    "channel-" + index,
                    "fingerprint-" + index,
                    index
            ).emitEvent());
        }

        assertEquals(ToolSelectionDiagnosticShaper.MAX_CHANNEL_STATES, shaper.channelStateCount());
        assertFalse(shaper.hasChannel("channel-0"));
        assertTrue(shaper.hasChannel("channel-16"));
    }

    @Test
    void regressedTickDoesNotCreateANegativeOrPrematureWindow() {
        ToolSelectionDiagnosticShaper shaper = new ToolSelectionDiagnosticShaper();

        assertTrue(shaper.evaluate(true, "selection", "same", 100L).emitEvent());
        ToolSelectionShapingDecision regressed = shaper.evaluate(
                true,
                "selection",
                "same",
                20L
        );

        assertFalse(regressed.emitSummary());

        ToolSelectionShapingDecision summary = shaper.evaluate(
                true,
                "selection",
                "same",
                300L
        );
        assertTrue(summary.emitSummary());
        assertEquals(2L, summary.suppressedRepeatCount());
        assertEquals(100L, summary.firstObservedTick());
        assertEquals(300L, summary.lastObservedTick());
    }

    @Test
    void semanticFingerprintExcludesPositionOnlyContextAndChangesForMeaningfulSelection() {
        String atFirstPosition = ToolSelectionSemanticFingerprint.selectionDecision(
                "SELECTED_TOOL",
                "minecraft:stone",
                "current=minecraft:stone_pickaxe",
                "selected=minecraft:iron_pickaxe",
                "speed=6.0"
        );
        String atSecondPosition = ToolSelectionSemanticFingerprint.selectionDecision(
                "SELECTED_TOOL",
                "minecraft:stone",
                "current=minecraft:stone_pickaxe",
                "selected=minecraft:iron_pickaxe",
                "speed=6.0"
        );
        String changedTool = ToolSelectionSemanticFingerprint.selectionDecision(
                "SELECTED_TOOL",
                "minecraft:stone",
                "current=minecraft:stone_pickaxe",
                "selected=minecraft:diamond_pickaxe",
                "speed=8.0"
        );

        assertEquals(atFirstPosition, atSecondPosition);
        assertNotEquals(atFirstPosition, changedTool);
    }

    @Test
    void boundedFingerprintStillDistinguishesLongSemanticValues() {
        String sharedPrefix = "x".repeat(200);

        String iron = ToolSelectionSemanticFingerprint.selectionDecision(
                "SELECTED_TOOL",
                "minecraft:stone",
                "current=minecraft:stone_pickaxe",
                sharedPrefix + "iron_pickaxe",
                "speed=6.0"
        );
        String diamond = ToolSelectionSemanticFingerprint.selectionDecision(
                "SELECTED_TOOL",
                "minecraft:stone",
                "current=minecraft:stone_pickaxe",
                sharedPrefix + "diamond_pickaxe",
                "speed=6.0"
        );

        assertNotEquals(iron, diamond);
        assertTrue(iron.length() <= 360);
        assertTrue(diamond.length() <= 360);
    }

    @Test
    void suppressionCounterSaturatesWithoutWrapping() {
        assertEquals(
                Long.MAX_VALUE,
                ToolSelectionSuppressionCounter.increment(Long.MAX_VALUE - 1L)
        );
        assertEquals(
                Long.MAX_VALUE,
                ToolSelectionSuppressionCounter.increment(Long.MAX_VALUE)
        );
    }
}
