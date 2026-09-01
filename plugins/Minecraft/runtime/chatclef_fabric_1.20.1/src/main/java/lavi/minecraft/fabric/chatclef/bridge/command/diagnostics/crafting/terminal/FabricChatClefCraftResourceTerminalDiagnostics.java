package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeBinding;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeDiagnostics;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceLifecycleClearKind;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourcePrimaryTerminationCause;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTaskFinishMatch;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalActivationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalDecision;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalKey;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceTerminalObservationBatch;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association.FabricChatClefCraftResourceAssociationScopeDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.common.FabricChatClefCraftResourceContainerProjectionSupport;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.FabricChatClefCraftResourceInteractionObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.requirement.FabricChatClefIronPickaxeRequirementProjectionDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target.FabricChatClefCraftResourceTargetScopeDiagnostics;

import java.util.Optional;

//20260901_kpopmodder: Compose two-phase terminal observers without owning command lifecycle.
public final class FabricChatClefCraftResourceTerminalDiagnostics {
    private static final CraftResourceTerminalDiagnosticsRegistry TERMINALS =
            new CraftResourceTerminalDiagnosticsRegistry();
    private static final FabricChatClefCraftResourceTerminalScopeRegistry SCOPES =
            new FabricChatClefCraftResourceTerminalScopeRegistry();
    private static final FabricChatClefCraftResourceTerminalEvidenceAssembler EVIDENCE =
            new FabricChatClefCraftResourceTerminalEvidenceAssembler();
    private static final FabricChatClefCraftResourceTerminalSummaryEmitter EMITTER =
            new FabricChatClefCraftResourceTerminalSummaryEmitter();

    private FabricChatClefCraftResourceTerminalDiagnostics() {
    }

    public static void observeActivation(
            IronPickaxeAcquisitionActivation activation,
            FabricChatClefCommandContext commandContext
    ) {
        if (activation == null || activation.binding().isEmpty() || commandContext == null) {
            return;
        }
        IronPickaxeAcquisitionScopeBinding scopeBinding = activation.binding().get();
        CraftResourceTerminalKey terminalKey = terminalKey(scopeBinding);
        CraftResourceTerminalActivation terminalActivation = TERMINALS.activate(
                terminalKey,
                scopeBinding.activatedAtClientTick(),
                scopeBinding.activatedAtMonotonicNanos()
        );
        if (terminalActivation.status() != CraftResourceTerminalActivationStatus.ACTIVATED
                && terminalActivation.status()
                != CraftResourceTerminalActivationStatus.ALREADY_ACTIVE) {
            return;
        }
        SCOPES.activate(scopeBinding, commandContext);
    }

    static Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact(
            FabricChatClefCommandContext context
    ) {
        return SCOPES.exact(context);
    }

    static void recordDetach(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String reason,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        SCOPES.recordFirstTerminalSource(binding.terminalKey(), sourceEvent);
        finish(TERMINALS.recordDetach(
                binding.terminalKey(), reason, clientTick, monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordPrimaryCause(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            CraftResourcePrimaryTerminationCause cause,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        SCOPES.recordFirstTerminalSource(binding.terminalKey(), sourceEvent);
        finish(TERMINALS.recordPrimaryCause(
                binding.terminalKey(), cause, clientTick, monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordExceptionEvidence(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String exceptionType,
            String exceptionMessage
    ) {
        SCOPES.recordExceptionEvidence(
                binding.terminalKey(),
                exceptionType,
                exceptionMessage
        );
    }

    static void recordOwnedRootCancellation(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String terminationKind,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        SCOPES.recordFirstTerminalSource(binding.terminalKey(), sourceEvent);
        finish(TERMINALS.recordOwnedRootCancellation(
                binding.terminalKey(),
                -1L,
                terminationKind,
                clientTick,
                monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordTaskFinished(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            CraftResourceTaskFinishMatch match,
            String terminationKind,
            boolean timedOut,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        if (match == CraftResourceTaskFinishMatch.MATCHING_ROOT) {
            SCOPES.recordFirstTerminalSource(binding.terminalKey(), sourceEvent);
        }
        finish(TERMINALS.recordTaskFinished(
                binding.terminalKey(),
                match,
                terminationKind,
                timedOut,
                clientTick,
                monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordClassification(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            String terminalDecisionReason,
            String resultStatus,
            String resultReason,
            String resultFidelity,
            String conclusion,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        finish(TERMINALS.recordClassification(
                binding.terminalKey(),
                terminalDecisionReason,
                resultStatus,
                resultReason,
                resultFidelity,
                conclusion,
                clientTick,
                monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordSend(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            FabricChatClefCraftResourceTerminalSendObservation send,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        finish(TERMINALS.recordSendOutcome(
                binding.terminalKey(),
                send.sendStatus(),
                send.deliveryStatus(),
                send.sent(),
                send.inFlight(),
                clientTick,
                monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordLifecycleCleared(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            CraftResourceLifecycleClearKind clearKind,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        finish(TERMINALS.recordLifecycleCleared(
                binding.terminalKey(), clearKind, clientTick, monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    static void recordQueueContextCleared(
            FabricChatClefCraftResourceTerminalScopeBinding binding,
            boolean mutationApplied,
            String unbindReason,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        finish(TERMINALS.recordQueueContextCleared(
                binding.terminalKey(),
                mutationApplied,
                unbindReason,
                clientTick,
                monotonicNanos
        ), sourceEvent, clientTick, monotonicNanos);
    }

    public static void observeRetention() {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        long clientTick = ChatClefDiagnostics.currentClientTickId();
        long monotonicNanos = System.nanoTime();
        CraftResourceTerminalObservationBatch batch = TERMINALS.observeRetentionAll(
                clientTick,
                monotonicNanos
        );
        for (CraftResourceTerminalDecision decision : batch.decisions()) {
            String sourceEvent = SCOPES.exact(decision.snapshot().key())
                    .map(FabricChatClefCraftResourceTerminalScopeBinding::firstTerminalSourceEvent)
                    .filter(value -> !value.isBlank())
                    .orElse("UNAVAILABLE_NO_AUTHORITATIVE_TERMINAL_SOURCE_EVENT");
            finish(Optional.of(decision), sourceEvent, clientTick, monotonicNanos);
        }
        SCOPES.expire(clientTick, monotonicNanos, batch.expiredTombstoneKeys());
    }

    public static void clearForModeOff() {
        TERMINALS.clearForModeOff();
        SCOPES.clear();
        FabricChatClefCraftResourceInteractionObserver.clearForModeOff();
    }

    private static CraftResourceTerminalKey terminalKey(
            IronPickaxeAcquisitionScopeBinding scopeBinding
    ) {
        return new CraftResourceTerminalKey(
                scopeBinding.key().commandSessionId(),
                scopeBinding.key().commandConnectionGeneration(),
                scopeBinding.key().commandRequestId(),
                scopeBinding.key().commandCorrelationId(),
                scopeBinding.key().rootAssignmentId()
        );
    }

    private static void finish(
            Optional<CraftResourceTerminalDecision> decision,
            String sourceEvent,
            long clientTick,
            long monotonicNanos
    ) {
        if (decision.isEmpty() || !decision.get().summaryRequested()) {
            return;
        }
        CraftResourceTerminalDecision terminalDecision = decision.get();
        Optional<FabricChatClefCraftResourceTerminalScopeBinding> terminalBinding =
                SCOPES.exact(terminalDecision.snapshot().key());
        if (terminalBinding.isEmpty()) {
            return;
        }
        FabricChatClefCraftResourceTerminalScopeBinding binding = terminalBinding.get();
        FabricChatClefCraftResourceTargetScopeDiagnostics.closeForCommandTerminal(binding.scopeKey());
        FabricChatClefCraftResourceTerminalEvidence evidence = EVIDENCE.assemble(binding);
        try {
            DiagnosticDispatchResult terminalDispatchResult = EMITTER.emit(
                    terminalDecision,
                    binding,
                    evidence,
                    sourceEvent
            );
            TERMINALS.recordAdmissionOutcome(
                    binding.terminalKey(),
                    terminalDispatchResult.admitted(),
                    terminalDispatchResult.emissionCompleted(),
                    clientTick,
                    monotonicNanos
            );
        } finally {
            SCOPES.markFinalized(binding.terminalKey(), clientTick, monotonicNanos);
            FabricChatClefIronPickaxeRequirementProjectionDiagnostics.retire(binding.scopeKey());
            FabricChatClefCraftResourceAssociationScopeDiagnostics.retire(binding.scopeKey());
            FabricChatClefCraftResourceContainerProjectionSupport.retire(binding.scopeKey());
            FabricChatClefCraftResourceInteractionObserver.retire(binding.scopeKey());
            FabricChatClefCraftResourceTargetScopeDiagnostics.retire(binding.scopeKey());
            IronPickaxeAcquisitionScopeDiagnostics.retire(
                    binding.scopeKey(),
                    clientTick,
                    monotonicNanos
            );
        }
    }
}
