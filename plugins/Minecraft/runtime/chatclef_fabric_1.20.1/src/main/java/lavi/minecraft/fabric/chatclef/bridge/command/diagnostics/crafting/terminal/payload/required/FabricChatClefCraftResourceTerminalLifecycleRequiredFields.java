package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceCoverageStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourcePrimaryTerminationCause;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalSnapshot;

import java.util.Map;

import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.unavailableIfBlank;
import static lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.FabricChatClefCraftResourceTerminalPayloadValue.unavailableLong;

//20260901_kpopmodder: Own required two-phase termination and delivery fields.
public final class FabricChatClefCraftResourceTerminalLifecycleRequiredFields {
    private FabricChatClefCraftResourceTerminalLifecycleRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            CraftResourceTerminalDecision decision,
            CraftResourceTerminalSnapshot snapshot) {
        fields.put("naturalTaskFinished", snapshot.naturalTaskFinished());
        fields.put(
                "thisOrChildTimedOutAtFinalization",
                snapshot.thisOrChildTimedOutAtFinalization()
        );
        fields.put(
                "thisOrChildTimedOutEverObserved",
                snapshot.thisOrChildTimedOutEverObserved()
        );
        fields.put(
                "firstTimedOutObservationTick",
                unavailableLong(snapshot.firstTimedOutObservationTick())
        );
        fields.put(
                "lastTimedOutObservationTick",
                unavailableLong(snapshot.lastTimedOutObservationTick())
        );
        fields.put("cancelInvocationId", unavailableLong(snapshot.cancelInvocationId()));
        fields.put("connectionDetached", snapshot.connectionDetachedObserved());
        fields.put("detachReason", unavailableIfBlank(snapshot.detachReason()));
        fields.put("primaryTerminationCause", snapshot.primaryTerminationCause().name());
        fields.put("taskTerminationKind", snapshot.taskTerminationKind());
        fields.put("terminalDecisionReason", snapshot.terminalDecisionReason());
        fields.put("classifiedResultStatus", snapshot.classifiedResultStatus());
        fields.put("classifiedResultReason", snapshot.classifiedResultReason());
        fields.put("classifiedResultFidelity", snapshot.classifiedResultFidelity());
        fields.put("evidenceConclusion", snapshot.evidenceConclusion());
        fields.put("terminalSent", snapshot.terminalSent());
        fields.put("resultSendStatus", snapshot.resultSendStatus().name());
        fields.put("resultDeliveryStatus", snapshot.resultDeliveryStatus().name());
        fields.put("lifecycleCleared", snapshot.lifecycleCleared());
        fields.put("queueContextCleared", snapshot.queueContextCleared());
        fields.put("contextUnbindReason", unavailableIfBlank(snapshot.contextUnbindReason()));
        fields.put("finalizationBoundary", decision.reason());
        fields.put("finalizationMode", snapshot.finalizationMode().name());
        fields.put("terminalEmissionAttempted", true);
        fields.put("terminalEmissionAdmitted", true);
        fields.put("terminalEmissionCompleted", true);
        fields.put("elapsedTicks", snapshot.elapsedTicks());
        fields.put("elapsedMs", snapshot.elapsedNanos() / 1_000_000L);
        fields.put("coverageStatus", snapshot.coverageStatus().name());
        fields.put("taskFinishObserved", snapshot.taskFinishObserved());
        fields.put("classificationObserved", snapshot.classificationObserved());
        fields.put("sendOutcomeObserved", snapshot.sendOutcomeObserved());
        fields.put("terminalSummaryRequestReason", decision.reason());
        fields.put("terminalEmissionAdmissionStatus", "ADMITTED_EMISSION_CALLS_RETURNED");
        fields.put(
                "diagnosticCaptureStatus",
                snapshot.coverageStatus() == CraftResourceCoverageStatus.COMPLETE
                        ? "complete"
                        : "partial"
        );
    }
}
