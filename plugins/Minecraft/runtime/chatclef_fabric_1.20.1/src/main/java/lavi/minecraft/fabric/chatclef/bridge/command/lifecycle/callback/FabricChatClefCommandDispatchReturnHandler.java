package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.callback;

//20260905_kpopmodder: Classify root ownership when ordinary command dispatch returns.

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.terminal.FabricChatClefCommandTerminalEvaluator;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

public final class FabricChatClefCommandDispatchReturnHandler {
    private final FabricChatClefRootOwnershipClassifier rootOwnershipClassifier;
    private final FabricChatClefCommandDiagnostics commandDiagnostics;
    private final FabricChatClefCommandTerminalEvaluator terminalEvaluator;

    public FabricChatClefCommandDispatchReturnHandler(
            FabricChatClefRootOwnershipClassifier rootOwnershipClassifier,
            FabricChatClefCommandDiagnostics commandDiagnostics,
            FabricChatClefCommandTerminalEvaluator terminalEvaluator
    ) {
        this.rootOwnershipClassifier = rootOwnershipClassifier;
        this.commandDiagnostics = commandDiagnostics;
        this.terminalEvaluator = terminalEvaluator;
    }

    public void handle(
            FabricChatClefCommandExecution execution,
            FabricChatClefTaskOwnershipEvidence taskAfterDispatch
    ) {
        FabricChatClefRootOwnershipClassification classification = rootOwnershipClassifier.classify(
                execution.taskBeforeDispatchEvidence(),
                taskAfterDispatch
        );
        execution.markDispatchReturned(taskAfterDispatch, classification);
        commandDiagnostics.info("dispatch_returned", execution);
        terminalEvaluator.evaluate(execution, false, null);
    }
}
