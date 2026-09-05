package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Preserve detach diagnostics API over focused log, projection, and snapshot owners.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefConnectionDetachDiagnostics {
    private final FabricChatClefConnectionDetachLogEmitter logEmitter;
    private final FabricChatClefConnectionDetachProjectionState projectionState;
    private final FabricChatClefConnectionDetachOwnershipSnapshotReader ownershipSnapshotReader;

    public FabricChatClefConnectionDetachDiagnostics(FabricChatClefBridgeDiagnostics diagnostics) {
        this(
                new FabricChatClefConnectionDetachLogEmitter(diagnostics),
                new FabricChatClefConnectionDetachProjectionState(),
                new FabricChatClefConnectionDetachOwnershipSnapshotReader()
        );
    }

    public FabricChatClefConnectionDetachDiagnostics(
            FabricChatClefConnectionDetachLogEmitter logEmitter,
            FabricChatClefConnectionDetachProjectionState projectionState,
            FabricChatClefConnectionDetachOwnershipSnapshotReader ownershipSnapshotReader
    ) {
        this.logEmitter = logEmitter;
        this.projectionState = projectionState;
        this.ownershipSnapshotReader = ownershipSnapshotReader;
    }

    public FabricChatClefTaskOwnershipSnapshot captureOwnership() {
        return ownershipSnapshotReader.capture();
    }

    public void terminalSendDeferred(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachedEvent event
    ) {
        logEmitter.terminalSendDeferred(context, event);
    }

    public void activeDetach(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachedEvent event,
            FabricChatClefConnectionDetachDecision decision
    ) {
        projectionState.record(decision);
        logEmitter.activeDetach(context, event, decision);
    }

    public void cancellationSkipped(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachDecision decision
    ) {
        logEmitter.cancellationSkipped(context, decision);
    }

    public void cancellationUnavailable() {
        logEmitter.cancellationUnavailable();
    }

    public void cancellingOwnedTask(String rootMatchReason) {
        logEmitter.cancellingOwnedTask(rootMatchReason);
    }

    public void detachedWithoutActive(
            FabricChatClefConnectionDetachResult detachResult,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore
    ) {
        if (!detachResult.changedQueueState()) {
            return;
        }
        logEmitter.detachedWithoutActive(
                detachResult,
                ownershipBefore,
                ownershipSnapshotReader.capture()
        );
    }

    public void queueCompletion(
            FabricChatClefCommandQueueCompletion completion,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            FabricChatClefConnectionDetachDecision decision
    ) {
        logEmitter.queueCompletion(
                completion,
                ownershipBefore,
                ownershipSnapshotReader.capture(),
                decision
        );
    }

    public String lastBoundRootOwnership() {
        return projectionState.lastBoundRootOwnership();
    }

    public String lastCancelAction() {
        return projectionState.lastCancelAction();
    }
}
