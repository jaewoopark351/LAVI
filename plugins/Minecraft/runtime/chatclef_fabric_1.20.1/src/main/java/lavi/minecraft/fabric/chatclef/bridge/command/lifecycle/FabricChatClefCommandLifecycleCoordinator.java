package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.FabricChatClefStopControlCommandLifecycle;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

//20260803_kpopmodder: Preserve the lifecycle API as a thin facade over focused owners.
public final class FabricChatClefCommandLifecycleCoordinator
        implements FabricChatClefStopControlCommandLifecycle {
    //20260905_kpopmodder: Retain exact reflection-based compatibility seams while focused owners mutate them.
    private final AtomicReference<FabricChatClefCommandExecution> activeExecution =
            new AtomicReference<>();
    private final FabricChatClefPreexistingIdleRootStabilityGate preexistingIdleRootStabilityGate =
            new FabricChatClefPreexistingIdleRootStabilityGate();
    private final FabricChatClefCommandLifecycleComponents components;

    public FabricChatClefCommandLifecycleCoordinator(
            FabricChatClefUserTaskFinishedObserver taskFinishedObserver,
            FabricChatClefCommandOutcomeClassifier outcomeClassifier,
            FabricChatClefCommandResultOutbox resultOutbox,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.components = new FabricChatClefCommandLifecycleComponents(
                taskFinishedObserver,
                outcomeClassifier,
                resultOutbox,
                resultSender,
                diagnostics,
                taskStateReader,
                activeExecution,
                preexistingIdleRootStabilityGate
        );
    }

    public void beginExecution(FabricChatClefCommandExecution execution) {
        components.executionStarter().begin(execution);
    }

    public void markDispatchReturned(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence taskAfterDispatch
    ) {
        components.dispatchReturnHandler().handle(execution, taskAfterDispatch);
    }

    public void markCommandFinish(
            FabricChatClefCommandExecution execution,
            Task taskAtFinish
    ) {
        components.finishCallbackHandler().handle(execution, taskAtFinish);
    }

    public void completeCommandException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        components.failureHandler().commandException(execution, exception, taskAtFailure);
    }

    public void completeDispatchException(
            FabricChatClefCommandExecution execution,
            Throwable exception,
            FabricChatClefTaskSnapshot taskAtFailure
    ) {
        components.failureHandler().dispatchException(execution, exception, taskAtFailure);
    }

    public void onEndClientTick(Optional<FabricChatClefCommandContext> activeContext) {
        components.tickCoordinator().onEndClientTick(activeContext);
    }

    public void completeActiveDeadline(FabricChatClefCommandContext context) {
        components.deadlineHandler().active(context);
    }

    public void completePendingDeadline(FabricChatClefCommandContext context) {
        components.deadlineHandler().pending(context);
    }

    public boolean hasActiveExecution(FabricChatClefCommandContext context) {
        return components.boundRootOwnershipReader().hasActiveExecution(context);
    }

    @Override
    public boolean bindUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity,
            Task currentTask
    ) {
        return components.stopControlLifecycle().bindUserStop(context, identity, currentTask);
    }

    @Override
    public boolean clearUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity
    ) {
        return components.stopControlLifecycle().clearUserStop(context, identity);
    }

    @Override
    public boolean sendUserStopCancellation(FabricChatClefCommandContext context) {
        return components.stopControlLifecycle().sendUserStopCancellation(context);
    }

    public FabricChatClefStopControlCommandLifecycle stopControlLifecycle() {
        return components.stopControlLifecycle();
    }

    @Override
    public boolean matchesBoundRootTask(FabricChatClefCommandContext context, Task candidateTask) {
        return components.boundRootOwnershipReader().matchesBoundRootTask(context, candidateTask);
    }

    public String boundRootMatchReason(FabricChatClefCommandContext context, Task candidateTask) {
        return components.boundRootOwnershipReader().boundRootMatchReason(context, candidateTask);
    }

    public String boundRootOwnershipForDetach(
            FabricChatClefCommandContext context,
            Task candidateTask
    ) {
        return components.boundRootOwnershipReader()
                .boundRootOwnershipForDetach(context, candidateTask);
    }

    public String detachCancelAction(FabricChatClefCommandContext context, Task candidateTask) {
        return components.boundRootOwnershipReader().detachCancelAction(context, candidateTask);
    }

    public boolean clearDetachedExecution(FabricChatClefCommandContext context, String reason) {
        return components.detachedExecutionRetirement().clear(context, reason);
    }
}
