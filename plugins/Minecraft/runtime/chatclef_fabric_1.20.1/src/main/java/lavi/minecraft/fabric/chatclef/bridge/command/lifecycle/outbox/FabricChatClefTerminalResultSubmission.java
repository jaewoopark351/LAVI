package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.outbox;

//20260905_kpopmodder: Sequence terminal commit, ownership validation, and transport.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandQueue;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResultSender;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.Supplier;

public final class FabricChatClefTerminalResultSubmission {
    private final FabricChatClefTerminalResultCommitter committer;
    private final FabricChatClefTerminalResultOwnershipValidator ownershipValidator;
    private final FabricChatClefTerminalResultTransport transport;

    public FabricChatClefTerminalResultSubmission(
            FabricChatClefCommandQueue commandQueue,
            FabricChatClefCommandResultSender resultSender,
            FabricChatClefCommandResultOutboxDiagnostics diagnostics
    ) {
        this.committer = new FabricChatClefTerminalResultCommitter();
        this.ownershipValidator = new FabricChatClefTerminalResultOwnershipValidator(
                commandQueue,
                diagnostics
        );
        this.transport = new FabricChatClefTerminalResultTransport(
                resultSender,
                diagnostics
        );
    }

    public boolean submitActive(
            FabricChatClefCommandExecution execution,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submit(
                execution.context(),
                execution.requestId(),
                execution.duplicateTerminalPayload("duplicate_terminal_result").toMap(),
                resultFactory,
                true
        );
    }

    public boolean submitActive(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submit(
                context,
                context.requestId(),
                context.ownershipPayload().toMap(),
                resultFactory,
                true
        );
    }

    public boolean submitPending(
            FabricChatClefCommandContext context,
            Supplier<FabricChatClefCommandResultPayload> resultFactory
    ) {
        return submit(
                context,
                context.requestId(),
                context.ownershipPayload().toMap(),
                resultFactory,
                false
        );
    }

    private boolean submit(
            FabricChatClefCommandContext context,
            String requestId,
            Object duplicateData,
            Supplier<FabricChatClefCommandResultPayload> resultFactory,
            boolean requireActive
    ) {
        FabricChatClefCommandResultPayload result = committer.commit(
                context,
                resultFactory
        );
        if (result == null) {
            return false;
        }
        boolean ownershipValid = requireActive
                ? ownershipValidator.validateActive(context, requestId, duplicateData)
                : ownershipValidator.validatePending(context, requestId, duplicateData);
        if (!ownershipValid) {
            return false;
        }
        return transport.submit(context, result);
    }
}
