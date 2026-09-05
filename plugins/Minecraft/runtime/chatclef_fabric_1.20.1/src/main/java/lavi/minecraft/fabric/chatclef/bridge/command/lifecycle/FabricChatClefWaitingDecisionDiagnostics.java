package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

//20260905_kpopmodder: Publish nonterminal ordinary-command decision diagnostics through a focused log gate.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefLifecycleDetailsPayload;

public final class FabricChatClefWaitingDecisionDiagnostics {
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefWaitingDecisionLogGate logGate;

    public FabricChatClefWaitingDecisionDiagnostics(
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.commandDiagnostics = commandDiagnostics;
        this.taskStateReader = taskStateReader;
        this.logGate = new FabricChatClefWaitingDecisionLogGate();
    }

    public void log(FabricChatClefCommandExecution execution, String reason) {
        long nowMs = System.currentTimeMillis();
        if (!logGate.shouldEmit(execution.requestId(), reason, nowMs)) {
            return;
        }
        Task currentTask = taskStateReader.currentTaskOrNull();
        commandDiagnostics.info(
                "waiting_for_terminal_condition",
                execution,
                FabricChatClefLifecycleDetailsPayload.waitingForTerminalCondition(
                        reason,
                        execution.boundRootRelationshipPayload("current_task", currentTask),
                        execution.boundRootMatchReason(currentTask),
                        taskStateReader.runtimePayload()
                )
        );
    }

    public void reset() {
        logGate.reset();
    }
}
