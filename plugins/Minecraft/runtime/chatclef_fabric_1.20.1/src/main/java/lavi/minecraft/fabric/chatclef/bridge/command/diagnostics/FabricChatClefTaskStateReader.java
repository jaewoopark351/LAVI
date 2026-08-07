package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskRuntimeObservationPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

//20260803_kpopmodder: Observe current ChatClef user task state without changing engine behavior.
public final class FabricChatClefTaskStateReader {
    private final FabricChatClefTaskOwnershipSnapshotReader ownershipSnapshotReader =
            new FabricChatClefTaskOwnershipSnapshotReader();

    public Task currentTaskOrNull() {
        try {
            return currentTaskOrThrow();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public FabricChatClefTaskSnapshot captureCurrentTaskSnapshot() {
        try {
            return FabricChatClefTaskSnapshot.capture(currentTaskOrThrow());
        } catch (Throwable error) {
            return FabricChatClefTaskSnapshot.unavailable(error);
        }
    }

    public FabricChatClefTaskRuntimeObservationPayload runtimePayload() {
        return FabricChatClefTaskRuntimeObservationPayload.of(
                Thread.currentThread().getName(),
                System.currentTimeMillis(),
                ChatClefDiagnostics.currentClientTickId(),
                captureCurrentTaskSnapshot(),
                ownershipSnapshot()
        );
    }

    public FabricChatClefTaskOwnershipSnapshot ownershipSnapshot() {
        return ownershipSnapshotReader.ownershipSnapshot();
    }

    private Task currentTaskOrThrow() {
        AltoClef mod = AltoClef.getInstance();
        if (mod == null || mod.getUserTaskChain() == null) {
            return null;
        }
        return mod.getUserTaskChain().getCurrentTask();
    }

}
