package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Isolate ordinary ChatClef command execution from tick and detach orchestration.

import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefOrdinaryCommandDispatchCoordinator {
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefOrdinaryCommandEngineReadiness engineReadiness;
    private final FabricChatClefOrdinaryCommandNormalizer commandNormalizer;
    private final FabricChatClefOrdinaryCommandDispatchDiagnostics dispatchDiagnostics;
    private final FabricChatClefOrdinaryCommandRunningResultPublisher runningResultPublisher;
    private final FabricChatClefOrdinaryCommandExecutorInvocation executorInvocation;

    public FabricChatClefOrdinaryCommandDispatchCoordinator(
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator,
            FabricChatClefBridgeDiagnostics diagnostics,
            FabricChatClefTaskStateReader taskStateReader
    ) {
        this.lifecycleCoordinator = lifecycleCoordinator;
        this.taskStateReader = taskStateReader;
        this.engineReadiness = new FabricChatClefOrdinaryCommandEngineReadiness();
        this.commandNormalizer = new FabricChatClefOrdinaryCommandNormalizer();
        this.dispatchDiagnostics = new FabricChatClefOrdinaryCommandDispatchDiagnostics(diagnostics);
        this.runningResultPublisher = new FabricChatClefOrdinaryCommandRunningResultPublisher(
                resultSender,
                dispatchDiagnostics
        );
        this.executorInvocation = new FabricChatClefOrdinaryCommandExecutorInvocation(
                lifecycleCoordinator,
                taskStateReader
        );
    }

    public boolean isEngineReady() {
        return engineReadiness.isReady();
    }

    public void dispatch(FabricChatClefCommandContext context) {
        FabricChatClefCommandRequest request = context.request();
        CommandExecutor executor = engineReadiness.commandExecutor();
        String command = commandNormalizer.normalize(executor, request.command);
        FabricChatClefTaskOwnershipEvidence taskBeforeDispatch = taskStateReader.ownershipEvidence();
        FabricChatClefCommandExecution execution = new FabricChatClefCommandExecution(
                context,
                command,
                taskBeforeDispatch
        );
        lifecycleCoordinator.beginExecution(execution);
        dispatchDiagnostics.started(context, request, command);
        runningResultPublisher.publish(context, execution);
        executorInvocation.invoke(executor, command, execution);
    }
}
