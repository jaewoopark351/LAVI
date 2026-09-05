package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.connection.FabricChatClefConnectionDetachCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlTickDispatcher;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import net.minecraft.client.MinecraftClient;

import java.util.Optional;
import java.util.function.Consumer;

//20260801_kpopmodder: Dispatch LAVI Fabric ChatClef commands only from the client tick.
public final class FabricChatClefCommandDispatcher {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefStopControlTickDispatcher stopControlTickDispatcher;
    private final FabricChatClefConnectionDetachCoordinator connectionDetachCoordinator;
    private final FabricChatClefOrdinaryCommandDispatchCoordinator ordinaryCommandDispatcher;

    public FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this(commandQueue, resultSender, lifecycleCoordinator, diagnostics, taskStateReader, null, null);
    }

    public FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefStopControlTickDispatcher stopControlTickDispatcher
    ) {
        this(
                commandQueue,
                resultSender,
                lifecycleCoordinator,
                diagnostics,
                taskStateReader,
                stopControlTickDispatcher,
                null
        );
    }

    FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            Consumer<String> detachedCommandCancellationAction
    ) {
        this(commandQueue, resultSender, lifecycleCoordinator, diagnostics, taskStateReader, null,
                detachedCommandCancellationAction);
    }

    private FabricChatClefCommandDispatcher(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader,
            FabricChatClefStopControlTickDispatcher stopControlTickDispatcher,
            Consumer<String> detachedCommandCancellationAction
    ) {
        this.commandQueue = commandQueue;
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.stopControlTickDispatcher = stopControlTickDispatcher;
        this.connectionDetachCoordinator = new FabricChatClefConnectionDetachCoordinator(
                commandQueue,
                lifecycleCoordinator,
                diagnostics,
                taskStateReader,
                detachedCommandCancellationAction
        );
        this.ordinaryCommandDispatcher = new FabricChatClefOrdinaryCommandDispatchCoordinator(
                resultSender,
                lifecycleCoordinator,
                diagnostics,
                taskStateReader
        );
    }

    public void onEndClientTick(MinecraftClient client) {
        long nowMs = System.currentTimeMillis();
        lifecycleCoordinator.onEndClientTick(commandQueue.activeContext());
        //20260905_kpopmodder: Give one admitted STOP exclusive tick ownership before ordinary dispatch.
        if (stopControlTickDispatcher != null
                && stopControlTickDispatcher.onEndClientTick(
                nowMs,
                ChatClefDiagnostics.currentClientTickId()
        )) {
            return;
        }
        if (connectionDetachCoordinator.processQueuedEvents()) {
            return;
        }
        Optional<FabricChatClefCommandContext> active = commandQueue.activeContext();
        if (active.isPresent()) {
            FabricChatClefCommandContext context = active.get();
            if (context.isDeadlineExceeded(nowMs)) {
                lifecycleCoordinator.completeActiveDeadline(context);
            }
            return;
        }
        Optional<FabricChatClefCommandContext> pending = commandQueue.peekPending();
        if (pending.isPresent() && pending.get().isDeadlineExceeded(nowMs)) {
            lifecycleCoordinator.completePendingDeadline(pending.get());
            return;
        }
        if (!ordinaryCommandDispatcher.isEngineReady()) {
            return;
        }
        commandQueue.pollForDispatch().ifPresent(ordinaryCommandDispatcher::dispatch);
    }

    boolean handleConnectionDetached(FabricChatClefConnectionDetachedEvent event, Task currentTask) {
        return connectionDetachCoordinator.handle(event, currentTask);
    }

    String lastBoundRootOwnershipForDetach() {
        return connectionDetachCoordinator.lastBoundRootOwnershipForDetach();
    }

    String lastDetachCancelAction() {
        return connectionDetachCoordinator.lastDetachCancelAction();
    }
}
