package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal;

//20260905_kpopmodder: Commit and submit terminal ordinary-command result payloads.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminalDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefPreexistingIdleRootStabilityObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Supplier;

public final class FabricChatClefCommandTerminalResultDispatcher {
    private final FabricChatClefCommandResultOutbox resultOutbox;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver;

    public FabricChatClefCommandTerminalResultDispatcher(
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver
    ) {
        this.resultOutbox = resultOutbox;
        this.diagnostics = diagnostics;
        this.commandDiagnostics = commandDiagnostics;
        this.stabilityObserver = stabilityObserver;
    }

    public void dispatchDecision(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandTerminalDecision decision
    ) {
        if (!execution.context().terminalSendReady(System.currentTimeMillis())) {
            return;
        }
        commandDiagnostics.info(
                "terminal_decision",
                execution,
                terminalDecisionDetails(execution, decision.reason())
        );
        if (execution.rootOwnershipClassification()
                == FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT) {
            stabilityObserver.reset();
        }
        dispatch(execution, decision::result);
        diagnostics.info(
                "terminal command decision request="
                        + execution.requestId()
                        + " reason="
                        + decision.reason()
        );
    }

    public void dispatch(
            FabricChatClefCommandExecution execution,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        boolean sendStarted = resultOutbox.sendTerminal(execution, resultFactory);
        commandDiagnostics.info(
                sendStarted ? "terminal_result_send_started" : "terminal_result_send_waiting",
                execution,
                FabricChatClefLifecycleDetailsPayload.terminalResult(false, false)
        );
    }

    private FabricChatClefLifecycleDetailsPayload terminalDecisionDetails(
            FabricChatClefCommandExecution execution,
            String decisionReason
    ) {
        if (execution.rootOwnershipClassification()
                != FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT) {
            return FabricChatClefLifecycleDetailsPayload.terminalDecision(decisionReason);
        }
        return FabricChatClefLifecycleDetailsPayload.preexistingIdleRoot(
                execution.rootOwnershipClassification(),
                execution.firstFinishCallbackObservation(),
                execution.finishCallbackDuplicateCount(),
                execution.preexistingIdleRootStabilityObservation(),
                "terminal_decision:" + decisionReason,
                null
        );
    }
}
