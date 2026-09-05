package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.evidence;

//20260905_kpopmodder: Own preexisting-idle-root stability observations for one active execution.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefPreexistingIdleRootStabilityGate;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

public final class FabricChatClefPreexistingIdleRootStabilityObserver {
    private final FabricChatClefPreexistingIdleRootStabilityGate stabilityGate;
    private final FabricChatClefTaskStateReader taskStateReader;

    public FabricChatClefPreexistingIdleRootStabilityObserver(
            FabricChatClefPreexistingIdleRootStabilityGate stabilityGate,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.stabilityGate = stabilityGate;
        this.taskStateReader = taskStateReader;
    }

    public void update(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandContext activeContext
    ) {
        if (execution.rootOwnershipClassification()
                != FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT) {
            return;
        }
        FabricChatClefTaskOwnershipEvidence currentEvidence = taskStateReader.ownershipEvidence();
        long nowNanos = System.nanoTime();
        long nowMs = System.currentTimeMillis();
        FabricChatClefStableRequestQuiescenceObservation observation = stabilityGate.observe(
                execution,
                currentEvidence,
                nowMs,
                nowNanos,
                lavi.minecraft.diagnostics.ChatClefDiagnostics.currentClientTickId(),
                activeContext
        );
        execution.markPreexistingIdleRootStabilityObservation(observation);
    }

    public void reset() {
        stabilityGate.reset();
    }
}
