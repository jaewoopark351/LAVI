package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260820_kpopmodder: Publish Java lifecycle evidence as running command_result diagnostics only.
public final class FabricChatClefNonterminalLifecycleEvidencePublisher {
    private static final String FINISH_STAGE = "finish_callback_observed_nonterminal";
    private static final String STABLE_STAGE = "stable_request_quiescence_observed";

    private final FabricChatClefCommandResultSender resultSender;
    private final Supplier<FabricChatClefTaskOwnershipEvidence> ownershipEvidenceSupplier;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final LongSupplier currentTimeMillisSupplier;
    private final LongSupplier nanoTimeSupplier;
    private final FabricChatClefStableRequestQuiescenceTracker quiescenceTracker =
            new FabricChatClefStableRequestQuiescenceTracker();
    private FabricChatClefCommandExecution trackedExecution;
    private boolean finishEvidenceSent;
    private boolean stableEvidenceSent;

    public FabricChatClefNonterminalLifecycleEvidencePublisher(
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefBridgeDiagnostics diagnostics
    ) {
        this(
                resultSender,
                taskStateReader::ownershipEvidence,
                diagnostics,
                System::currentTimeMillis,
                System::nanoTime
        );
    }

    FabricChatClefNonterminalLifecycleEvidencePublisher(
            FabricChatClefCommandResultSender resultSender,
            Supplier<FabricChatClefTaskOwnershipEvidence> ownershipEvidenceSupplier,
            FabricChatClefBridgeDiagnostics diagnostics,
            LongSupplier currentTimeMillisSupplier,
            LongSupplier nanoTimeSupplier
    ) {
        this.resultSender = resultSender;
        this.ownershipEvidenceSupplier = ownershipEvidenceSupplier;
        this.diagnostics = diagnostics;
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
        this.nanoTimeSupplier = nanoTimeSupplier;
    }

    public void reset() {
        trackedExecution = null;
        finishEvidenceSent = false;
        stableEvidenceSent = false;
        quiescenceTracker.reset();
    }

    public void publishIfEligible(
            FabricChatClefCommandExecution execution,
            String waitingReason,
            FabricChatClefCommandContext activeContext
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
        FabricChatClefTaskOwnershipEvidence currentEvidence = ownershipEvidenceSupplier.get();
        if (currentEvidence == null) {
            currentEvidence = FabricChatClefTaskOwnershipEvidence.empty();
        }
        long nowNanos = nanoTimeSupplier.getAsLong();
        long nowMs = currentTimeMillisSupplier.getAsLong();
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
                        clientTickId,
                        activeContext
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
        quiescenceTracker.reset();
    }

    private void publish(
            FabricChatClefCommandExecution execution,
            String stage,
            String waitingReason,
            FabricChatClefTaskSnapshot currentTaskSnapshot,
            FabricChatClefStableRequestQuiescenceObservation quiescence
    ) {
        int sequence = execution.nextLifecycleEvidenceSequence();
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
