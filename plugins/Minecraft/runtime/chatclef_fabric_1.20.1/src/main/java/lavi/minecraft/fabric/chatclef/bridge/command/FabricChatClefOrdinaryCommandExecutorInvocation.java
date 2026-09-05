package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Own only the ordinary CommandExecutor invocation and callback boundary.

import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;

public final class FabricChatClefOrdinaryCommandExecutorInvocation {
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefTaskStateReader taskStateReader;

    public FabricChatClefOrdinaryCommandExecutorInvocation(
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.taskStateReader = taskStateReader;
    }

    public void invoke(
            CommandExecutor executor,
            String command,
            FabricChatClefCommandExecution execution
    ) {
        try {
            execution.openExecutorExecuteInvocation();
            executor.execute(
                    command,
                    () -> lifecycleCoordinator.markCommandFinish(
                            execution,
                            taskStateReader.currentTaskOrNull()
                    ),
                    exception -> lifecycleCoordinator.completeCommandException(
                            execution,
                            exception,
                            taskStateReader.captureCurrentTaskSnapshot()
                    )
            );
            execution.closeExecutorExecuteInvocation();
            lifecycleCoordinator.markDispatchReturned(execution, taskStateReader.ownershipEvidence());
        } catch (Throwable error) {
            execution.closeExecutorExecuteInvocation();
            lifecycleCoordinator.completeDispatchException(
                    execution,
                    error,
                    taskStateReader.captureCurrentTaskSnapshot()
            );
        }
    }
}
