package lavi.minecraft.diagnostics.toolselect;

import adris.altoclef.AltoClef;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.session.lifecycle.registration.DiagnosticOwnerRegistration;
import lavi.minecraft.diagnostics.toolselect.lifecycle.ToolSelectionDiagnosticStateObserver;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionDiagnosticShaper;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionSemanticFingerprint;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionShapingDecision;
import lavi.minecraft.diagnostics.toolselect.shaping.ToolSelectionSuppressionSummaryEmitter;
import lavi.minecraft.diagnostics.toolselect.support.ToolCandidateDiagnosticFormatter;
import lavi.minecraft.diagnostics.toolselect.support.ToolDiagnosticFormatter;
import lavi.minecraft.diagnostics.toolselect.support.ToolSavePolicyDiagnostics;
import lavi.minecraft.diagnostics.toolselect.support.ToolTargetDiagnosticFields;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;

import java.util.StringJoiner;

//20260801_kpopmodder: Observe the actual StorageHelper best-tool filtering boundary without changing selection behavior.
public final class BestToolSlotDiagnostics {
    public static final String DECISION_HARDNESS_ZERO_USE_EQUIP_SLOT = "HARDNESS_ZERO_USE_EQUIP_SLOT";
    public static final String DECISION_NO_ELIGIBLE_TOOL = "NO_ELIGIBLE_TOOL";
    public static final String DECISION_SELECTED_TOOL = "SELECTED_TOOL";

    private static final String SHAPING_CHANNEL = "best_tool_slot";
    private static final ToolSelectionDiagnosticShaper SHAPER =
            new ToolSelectionDiagnosticShaper();
    private static final ToolSelectionDiagnosticStateObserver OFF_STATE_OBSERVER =
            new ToolSelectionDiagnosticStateObserver(SHAPER::clearForModeOff);
    private static final DiagnosticOwnerRegistration OWNER =
            new DiagnosticOwnerRegistration("best_tool_slot", SHAPER::clearForModeOff);

    static {
        ChatClefDiagnostics.registerSessionLifecycleOwner(OWNER, OFF_STATE_OBSERVER);
    }

    private BestToolSlotDiagnostics() {
    }

    public static void disableOwner(String reason) {
        OWNER.disable(reason);
    }

    public static Scan start(BlockState targetState) {
        Scan[] result = {Scan.disabled()};
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> OWNER.runIfAvailable(
                () -> result[0] = new Scan(true, targetState, OFF_STATE_OBSERVER.generation())));
        return result[0];
    }

    public static final class Scan {
        private final boolean enabled;
        private final long generation;
        private final BlockState targetState;
        private final StringJoiner candidates = new StringJoiner(",", "[", "]");
        private int candidateCount;
        private int emittedCandidates;
        private int toolCandidateCount;
        private int defaultSuitableToolCount;
        private int savedSuitableToolCount;
        private int eligibleToolCount;
        private int shearsCandidateCount;
        private int effectiveShearsCount;

        private Scan(boolean enabled, BlockState targetState, long generation) {
            this.enabled = enabled;
            this.targetState = targetState;
            this.generation = generation;
        }

        private static Scan disabled() {
            return new Scan(false, null, -1L);
        }

        public void observeToolCandidate(AltoClef mod,
                                         Slot slot,
                                         ItemStack stack,
                                         boolean defaultSuitable,
                                         boolean shouldSave,
                                         double speed,
                                         boolean becameBest) {
            runCurrent(() -> observeToolCandidateEligible(mod, slot, stack, defaultSuitable, shouldSave, speed, becameBest));
        }

        private void observeToolCandidateEligible(AltoClef mod, Slot slot, ItemStack stack,
                                                  boolean defaultSuitable, boolean shouldSave,
                                                  double speed, boolean becameBest) {
            candidateCount++;
            toolCandidateCount++;
            if (defaultSuitable) {
                defaultSuitableToolCount++;
            }
            if (defaultSuitable && shouldSave) {
                savedSuitableToolCount++;
            }
            if (defaultSuitable && !shouldSave) {
                eligibleToolCount++;
            }
            if (emittedCandidates >= ToolDiagnosticFormatter.MAX_CANDIDATES) {
                return;
            }

            String outcome = toolOutcome(defaultSuitable, shouldSave, becameBest);
            String saveDecision = defaultSuitable
                    ? ToolSavePolicyDiagnostics.observedDecision(mod, stack, targetState, shouldSave)
                    : "not_evaluated#reason=NOT_DEFAULT_SUITABLE";
            candidates.add(ToolCandidateDiagnosticFormatter.bestToolCandidate(
                    slot,
                    ToolDiagnosticFormatter.toolStackDetails(stack),
                    defaultSuitable,
                    defaultSuitable ? Boolean.toString(shouldSave) : "not_evaluated",
                    outcome,
                    saveDecision,
                    ToolDiagnosticFormatter.speedValue(speed)));
            emittedCandidates++;
        }

        public void observeShearsCandidate(Slot slot, ItemStack stack, boolean effective, boolean selected) {
            runCurrent(() -> observeShearsCandidateEligible(slot, stack, effective, selected));
        }

        private void observeShearsCandidateEligible(Slot slot, ItemStack stack, boolean effective, boolean selected) {
            candidateCount++;
            shearsCandidateCount++;
            if (effective) {
                effectiveShearsCount++;
            }
            if (emittedCandidates >= ToolDiagnosticFormatter.MAX_CANDIDATES) {
                return;
            }
            candidates.add(ToolCandidateDiagnosticFormatter.shearsCandidate(
                    slot,
                    ToolDiagnosticFormatter.toolStackDetails(stack),
                    effective,
                    selected));
            emittedCandidates++;
        }

        public void logReturn(Slot bestToolSlot, String decisionReason, double highestSpeed) {
            runCurrent(() -> logReturnEligible(bestToolSlot, decisionReason, highestSpeed));
        }

        private void runCurrent(Runnable diagnostic) {
            if (!enabled) {
                return;
            }
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> OWNER.runIfAvailable(() -> {
                if (generation == OFF_STATE_OBSERVER.generation()) {
                    diagnostic.run();
                }
            }));
        }

        private void logReturnEligible(Slot bestToolSlot,
                                       String decisionReason,
                                       double highestSpeed) {
            String fingerprint = fingerprint(bestToolSlot, decisionReason, highestSpeed);
            ToolSelectionShapingDecision shaping = SHAPER.evaluate(
                    true,
                    SHAPING_CHANNEL,
                    fingerprint,
                    ChatClefDiagnostics.currentClientTickId()
            );
            ToolSelectionSuppressionSummaryEmitter.emit(
                    "BEST_TOOL_SLOT_DECISION_REPEAT_SUMMARY",
                    "storage_helper_get_best_tool_slot_repeat_summary",
                    SHAPING_CHANNEL,
                    shaping
            );
            if (!shaping.emitEvent()) {
                return;
            }

            ChatClefDiagnostics.logBoundary("BEST_TOOL_SLOT_DECISION", "storage_helper_get_best_tool_slot", null,
                    "decisionReason", decisionReason,
                    "targetBlockState", ToolTargetDiagnosticFields.blockState(targetState),
                    "targetBlockId", ToolTargetDiagnosticFields.blockId(targetState),
                    "targetRequiresTool", ToolTargetDiagnosticFields.requiresTool(targetState),
                    "minimumMiningRequirement", ToolTargetDiagnosticFields.minimumMiningRequirement(targetState),
                    "selectedSlot", ChatClefDiagnostics.slotSummary(bestToolSlot),
                    "selectedStack", ToolDiagnosticFormatter.toolStackDetails(ToolDiagnosticFormatter.slotStack(bestToolSlot)),
                    "selectedSpeed", ToolDiagnosticFormatter.speedValue(highestSpeed),
                    "candidateCount", candidateCount,
                    "toolCandidateCount", toolCandidateCount,
                    "defaultSuitableToolCount", defaultSuitableToolCount,
                    "savedSuitableToolCount", savedSuitableToolCount,
                    "eligibleToolCount", eligibleToolCount,
                    "shearsCandidateCount", shearsCandidateCount,
                    "effectiveShearsCount", effectiveShearsCount,
                    "truncatedCandidateCount", Math.max(0, candidateCount - emittedCandidates),
                    "candidateSummary", candidates.toString(),
                    "priorSuppressedRepeatCount", shaping.suppressedRepeatCount(),
                    "priorSuppressionFirstObservedTick", shaping.firstObservedTick(),
                    "priorSuppressionLastObservedTick", shaping.lastObservedTick(),
                    "priorSemanticFingerprint", shaping.summaryFingerprint());
        }

        private String fingerprint(Slot bestToolSlot, String decisionReason, double highestSpeed) {
            String selectedTool = ChatClefDiagnostics.slotSummary(bestToolSlot)
                    + "#" + ToolDiagnosticFormatter.bestToolStackFingerprint(
                    ToolDiagnosticFormatter.slotStack(bestToolSlot)
            );
            String decisionDetails = ToolDiagnosticFormatter.speedValue(highestSpeed)
                    + "#candidateCount=" + candidateCount
                    + "#toolCandidateCount=" + toolCandidateCount
                    + "#defaultSuitableToolCount=" + defaultSuitableToolCount
                    + "#savedSuitableToolCount=" + savedSuitableToolCount
                    + "#eligibleToolCount=" + eligibleToolCount
                    + "#shearsCandidateCount=" + shearsCandidateCount
                    + "#effectiveShearsCount=" + effectiveShearsCount
                    + "#candidates=" + candidates;
            return ToolSelectionSemanticFingerprint.selectionDecision(
                    decisionReason,
                    String.valueOf(ToolTargetDiagnosticFields.blockId(targetState)),
                    "best_tool_scan",
                    selectedTool,
                    decisionDetails
            );
        }
    }

    private static String toolOutcome(boolean defaultSuitable, boolean shouldSave, boolean becameBest) {
        if (!defaultSuitable) {
            return "SKIP_NOT_SUITABLE";
        }
        if (shouldSave) {
            return "SKIP_SHOULD_SAVE";
        }
        return becameBest ? "ELIGIBLE_NEW_BEST" : "ELIGIBLE_NOT_BEST";
    }

}
