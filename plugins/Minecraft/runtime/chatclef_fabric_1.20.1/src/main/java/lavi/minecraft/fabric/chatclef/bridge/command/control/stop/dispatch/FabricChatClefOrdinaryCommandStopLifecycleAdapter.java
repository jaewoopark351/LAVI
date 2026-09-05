package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch;

//20260905_kpopmodder: Adapt exact ordinary-command lifecycle ownership to the STOP control boundary.

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlIdentity;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefOriginalCancellationResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;

import java.util.function.Supplier;

public final class FabricChatClefOrdinaryCommandStopLifecycleAdapter
        implements FabricChatClefStopControlCommandLifecycle {
    private final Supplier<FabricChatClefCommandExecution> activeExecution;
    private final FabricChatClefCommandResultOutbox resultOutbox;
    private final FabricChatClefOriginalCancellationResultFactory pendingCancellationFactory;

    public FabricChatClefOrdinaryCommandStopLifecycleAdapter(
            Supplier<FabricChatClefCommandExecution> activeExecution,
            FabricChatClefCommandResultOutbox resultOutbox
    ) {
        this.activeExecution = activeExecution;
        this.resultOutbox = resultOutbox;
        this.pendingCancellationFactory = new FabricChatClefOriginalCancellationResultFactory();
    }

    @Override
    public boolean bindUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity,
            Task currentTask
    ) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        return execution != null
                && execution.context() == context
                && !context.terminalPayloadCommitted()
                && execution.bindUserStop(identity, currentTask);
    }

    @Override
    public boolean clearUserStop(
            FabricChatClefCommandContext context,
            FabricChatClefStopControlIdentity identity
    ) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        return execution != null
                && execution.context() == context
                && execution.clearUserStop(identity);
    }

    @Override
    public boolean sendUserStopCancellation(FabricChatClefCommandContext context) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        if (execution != null && execution.context() == context && execution.userStopBound()) {
            return resultOutbox.sendTerminal(execution, execution::cancelledFromUserStop);
        }
        return resultOutbox.sendPendingTerminal(
                context,
                () -> pendingCancellationFactory.create(context)
        );
    }

    @Override
    public boolean matchesBoundRootTask(FabricChatClefCommandContext context, Task candidateTask) {
        FabricChatClefCommandExecution execution = activeExecution.get();
        return execution != null
                && execution.context() == context
                && execution.matchesBoundRootTask(candidateTask);
    }
}
