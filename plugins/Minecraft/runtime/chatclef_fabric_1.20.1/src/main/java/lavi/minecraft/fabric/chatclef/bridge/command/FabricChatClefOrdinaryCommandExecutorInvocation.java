package lavi.minecraft.fabric.chatclef.bridge.command;

//20260905_kpopmodder: Own only the ordinary CommandExecutor invocation and callback boundary.

import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;

public final class FabricChatClefOrdinaryCommandExecutorInvocation {
    private final FabricChatClefCommandLifecycleCoordinator lifecycleCoordinator;
    private final FabricChatClefTaskStateReader taskStateReader;
    private final FabricChatClefOrdinaryCommandDispatchDiagnostics diagnostics =
            new FabricChatClefOrdinaryCommandDispatchDiagnostics(new lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics());

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
        String prefixlessCommand = command.substring(executor.getCommandPrefix().length());
        try (var instantCapture = lavi.minecraft.command.result.instant.InstantCommandResultCapture.begin(prefixlessCommand);
             var attackScope = lavi.minecraft.command.attack.KoreanAttackExecutionScope.begin(prefixlessCommand, execution.context().request().metadata)) {
            String catalogueRejection = lavi.minecraft.fabric.chatclef.bridge.catalogue.admission.FabricChatClefRuntimeCatalogueAdmission.rejectionReason(
                    execution.context().request().metadata, execution.context().sessionId(),
                    lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueSnapshotStore.current());
            if (catalogueRejection != null) {
                diagnostics.catalogueAdmissionRejected(execution.context(), catalogueRejection);
                lifecycleCoordinator.completeCommandException(execution,
                        new lavi.minecraft.fabric.chatclef.bridge.catalogue.admission.FabricChatClefRuntimeCatalogueAdmissionException(catalogueRejection),
                        taskStateReader.captureCurrentTaskSnapshot());
                return;
            }
            execution.openExecutorExecuteInvocation();
            if (lavi.minecraft.command.attack.KoreanAttackExecutionScope.mobOnly())
                diagnostics.koreanAttackBound(execution.context());
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
            //20260915_kpopmodder: Observe settings and native query outcomes inside this exact invocation.
            lavi.minecraft.command.result.instant.InstantSettingResultReader.observe(prefixlessCommand);
            execution.attachInstantResult(instantCapture.result());
            diagnostics.instantResultObserved(execution.context(), instantCapture.result());
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
