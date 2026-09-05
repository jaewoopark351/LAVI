package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Retire command ownership for one accepted connection-detach event.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;

import java.util.Optional;

public final class FabricChatClefConnectionDetachRetirement {
    private final FabricChatClefCommandQueue commandQueue;
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefConnectionDetachDecisionResolver decisionResolver;
    private final FabricChatClefDetachedCommandCancellation cancellation;
    private final FabricChatClefConnectionDetachDiagnostics diagnostics;

    public FabricChatClefConnectionDetachRetirement(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefConnectionDetachDecisionResolver decisionResolver,
            FabricChatClefDetachedCommandCancellation cancellation,
            FabricChatClefConnectionDetachDiagnostics diagnostics
    ) {
        this.commandQueue = commandQueue;
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.decisionResolver = decisionResolver;
        this.cancellation = cancellation;
        this.diagnostics = diagnostics;
    }

    public boolean retire(FabricChatClefConnectionDetachedEvent event, Task currentTask) {
        FabricChatClefTaskOwnershipSnapshot ownershipBefore = diagnostics.captureOwnership();
        FabricChatClefConnectionDetachResult detachResult = commandQueue.markConnectionDetached(event);
        Optional<FabricChatClefCommandContext> detachedActive = detachResult.detachedActive();
        if (detachedActive.isEmpty()) {
            if (detachResult.pendingInFlightRetainedCount() > 0) {
                return true;
            }
            diagnostics.detachedWithoutActive(detachResult, ownershipBefore);
            return false;
        }
        FabricChatClefCommandContext context = detachedActive.get();
        if (context.terminalSendInFlight()) {
            if (context.markTerminalDetachDeferLogged()) {
                diagnostics.terminalSendDeferred(context, event);
            }
            return true;
        }
        FabricChatClefConnectionDetachDecision decision = decisionResolver.resolve(context, currentTask);
        diagnostics.activeDetach(context, event, decision);
        if (decision.ownsCurrentTask()) {
            cancellation.cancel(decision.rootMatchReason());
        } else {
            diagnostics.cancellationSkipped(context, decision);
        }
        lifecycleCoordinator.onEndClientTick(commandQueue.activeContext());
        lifecycleCoordinator.clearDetachedExecution(
                context,
                event.reason() + ":" + decision.rootMatchReason()
        );
        FabricChatClefCommandQueueCompletion completion = commandQueue.clearDetachedActive(
                context,
                decision.ownsCurrentTask()
                        ? "connection_detached_task_cancelled"
                        : "connection_detached_task_not_owned"
        );
        diagnostics.queueCompletion(completion, ownershipBefore, decision);
        return false;
    }
}
