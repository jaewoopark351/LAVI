package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

//20260820_kpopmodder: Publish Java lifecycle evidence as running command_result diagnostics only.
public final class FabricChatClefNonterminalLifecycleEvidencePublisher {
    private static final String FINISH_STAGE = "finish_callback_observed_nonterminal";
    private static final String STABLE_STAGE = "stable_request_quiescence_observed";

    private final FabricChatClefCommandResultSender resultSender;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefStableRequestQuiescenceTracker quiescenceTracker =
            new FabricChatClefStableRequestQuiescenceTracker();
    private FabricChatClefCommandExecution trackedExecution;
    private boolean finishEvidenceSent;
    private boolean stableEvidenceSent;
    private int evidenceSequence;

    public FabricChatClefNonterminalLifecycleEvidencePublisher(
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this.resultSender = resultSender;
        this.taskStateReader = taskStateReader;
        this.diagnostics = diagnostics;
    }

    public void reset() {
        trackedExecution = null;
        finishEvidenceSent = false;
        stableEvidenceSent = false;
        evidenceSequence = 0;
        quiescenceTracker.reset();
    }

    public void publishIfEligible(
            FabricChatClefCommandExecution execution,
            String waitingReason
    ) {
        if (execution == null) {
            reset();
            return;
        }
        syncExecution(execution);
        if (!execution.finishCallbackReceived()) {
            return;
        }
        if (finishEvidenceSent && stableEvidenceSent) {
            return;
        }
        long nowMs = System.currentTimeMillis();
        long nowNanos = System.nanoTime();
        FabricChatClefTaskOwnershipEvidence currentEvidence = taskStateReader.ownershipEvidence();
        if (currentEvidence == null) {
            currentEvidence = FabricChatClefTaskOwnershipEvidence.empty();
        }
        long clientTickId = currentEvidence.available()
                ? currentEvidence.capturedClientTick()
                : ChatClefDiagnostics.currentClientTickId();
        FabricChatClefTaskSnapshot currentTaskSnapshot = currentEvidence.rootTaskSnapshot();
        FabricChatClefStableRequestQuiescenceObservation quiescence =
                quiescenceTracker.observe(
                        execution,
                        currentEvidence,
                        waitingReason,
                        nowMs,
                        nowNanos,
                        clientTickId
                );
        if (!finishEvidenceSent) {
            publish(execution, FINISH_STAGE, waitingReason, currentTaskSnapshot, quiescence);
            finishEvidenceSent = true;
        }
        if (!stableEvidenceSent && quiescence.qualified()) {
            publish(execution, STABLE_STAGE, waitingReason, currentTaskSnapshot, quiescence);
            stableEvidenceSent = true;
        }
    }

    private void syncExecution(FabricChatClefCommandExecution execution) {
        if (trackedExecution == execution) {
            return;
        }
        trackedExecution = execution;
        finishEvidenceSent = false;
        stableEvidenceSent = false;
        evidenceSequence = 0;
        quiescenceTracker.reset();
    }

    private void publish(
            FabricChatClefCommandExecution execution,
            String stage,
            String waitingReason,
            FabricChatClefTaskSnapshot currentTaskSnapshot,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        int sequence = ++evidenceSequence;
        FabricChatClefCommandResultSendSubmission submission = resultSender.sendCommandResult(
                execution.context(),
                execution.runningLifecycleEvidenceResult(
                        stage,
                        sequence,
                        waitingReason,
                        currentTaskSnapshot,
                        quiescence
                )
        );
        if (!submission.acceptedForAsyncSend()) {
            diagnostics.warn(
                    "nonterminal lifecycle evidence send failed request="
                            + execution.requestId()
                            + " stage="
                            + stage
                            + " evidence_sequence="
                            + sequence
                            + " outcome="
                            + submission.diagnosticMessage()
            );
            return;
        }
        diagnostics.info(
                "nonterminal lifecycle evidence send started request="
                        + execution.requestId()
                        + " stage="
                        + stage
                        + " evidence_sequence="
                        + sequence
                        + " quiescence_qualified="
                        + quiescence.qualified()
        );
    }
}
