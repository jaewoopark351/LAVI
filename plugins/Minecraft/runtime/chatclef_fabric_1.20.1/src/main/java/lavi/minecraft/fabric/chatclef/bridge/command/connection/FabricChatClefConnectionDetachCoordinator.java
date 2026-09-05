package lavi.minecraft.fabric.chatclef.bridge.command.connection;

//20260905_kpopmodder: Preserve detach handling as a thin facade over drain, retirement, cancellation, and diagnostics collaborators.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefAltoClefDetachedCommandCancellation;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachDecisionResolver;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachEventDrainer;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefConnectionDetachRetirement;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.detach.FabricChatClefDetachedCommandCancellation;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.function.Consumer;

public final class FabricChatClefConnectionDetachCoordinator {
    private final FabricChatClefConnectionDetachEventDrainer eventDrainer;
    private final FabricChatClefConnectionDetachDiagnostics detachDiagnostics;

    public FabricChatClefConnectionDetachCoordinator(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            Consumer<String> detachedCommandCancellationAction
    ) {
        this.detachDiagnostics = new FabricChatClefConnectionDetachDiagnostics(diagnostics);
        FabricChatClefDetachedCommandCancellation cancellation = detachedCommandCancellationAction == null
                ? new FabricChatClefAltoClefDetachedCommandCancellation(detachDiagnostics)
                : detachedCommandCancellationAction::accept;
        FabricChatClefConnectionDetachRetirement retirement =
                new FabricChatClefConnectionDetachRetirement(
                        commandQueue,
                        lifecycleCoordinator,
                        new FabricChatClefConnectionDetachDecisionResolver(lifecycleCoordinator),
                        cancellation,
                        detachDiagnostics
                );
        this.eventDrainer = new FabricChatClefConnectionDetachEventDrainer(
                commandQueue,
                taskStateReader,
                retirement
        );
    }

    public boolean processQueuedEvents() {
        return eventDrainer.drain();
    }

    public boolean handle(FabricChatClefConnectionDetachedEvent event, Task currentTask) {
        return eventDrainer.handle(event, currentTask);
    }

    public String lastBoundRootOwnershipForDetach() {
        return detachDiagnostics.lastBoundRootOwnership();
    }

    public String lastDetachCancelAction() {
        return detachDiagnostics.lastCancelAction();
    }
}
