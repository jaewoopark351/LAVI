package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation;

//20260905_kpopmodder: Associate one TaskFinished observation with the active ordinary execution.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.details.FabricChatClefTaskFinishedEventDetailsPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence.FabricChatClefPreexistingIdleRootStabilityObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.execution.FabricChatClefActiveExecutionStore;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalEvaluator;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefTaskTerminationObservationHandler {
    private final FabricChatClefActiveExecutionStore executionStore;
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver;
    private final FabricChatClefCommandTerminalEvaluator terminalEvaluator;

    public FabricChatClefTaskTerminationObservationHandler(
            FabricChatClefActiveExecutionStore executionStore,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefPreexistingIdleRootStabilityObserver stabilityObserver,
            FabricChatClefCommandTerminalEvaluator terminalEvaluator
    ) {
        this.executionStore = executionStore;
        this.diagnostics = diagnostics;
        this.commandDiagnostics = commandDiagnostics;
        this.taskStateReader = taskStateReader;
        this.stabilityObserver = stabilityObserver;
        this.terminalEvaluator = terminalEvaluator;
    }

    public void handle(FabricChatClefCommandTerminationObservation observation) {
        FabricChatClefCommandExecution execution = executionStore.current();
        if (execution == null) {
            diagnostics.info(
                    "ignored TaskFinishedEvent without active LAVI command data="
                            + observation.toMap()
            );
            return;
        }
        FabricChatClefRootOwnershipClassification classification = execution.rootOwnershipClassification();
        if (classification == FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT
                && !execution.hasBoundRootTask()) {
            stabilityObserver.reset();
            logUnbound(execution, observation, "unbound_audit_only");
            return;
        }
        if (classification == FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN) {
            logUnbound(execution, observation, "ownership_unknown_audit_only");
            return;
        }
        execution.markTaskFinishedObservation(observation);
        FabricChatClefTaskFinishedEventDetailsPayload details = FabricChatClefTaskFinishedEventDetailsPayload.of(
                observation,
                execution.matchesBoundRootTask(observation),
                execution.boundRootRelationshipPayload("event_task", observation.task()),
                execution.boundRootMatchReason(observation.task()),
                execution.finishCallbackReceived(),
                taskStateReader.runtimePayload()
        );
        commandDiagnostics.info("task_finished_event_received", execution, details);
        terminalEvaluator.evaluate(execution, false, null);
    }

    private void logUnbound(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandTerminationObservation observation,
            String association
    ) {
        commandDiagnostics.info(
                "task_finished_event_unbound_audit",
                execution,
                FabricChatClefLifecycleDetailsPayload.preexistingIdleRoot(
                        execution.rootOwnershipClassification(),
                        execution.firstFinishCallbackObservation(),
                        execution.finishCallbackDuplicateCount(),
                        execution.preexistingIdleRootStabilityObservation(),
                        association,
                        observation
                )
        );
    }
}
