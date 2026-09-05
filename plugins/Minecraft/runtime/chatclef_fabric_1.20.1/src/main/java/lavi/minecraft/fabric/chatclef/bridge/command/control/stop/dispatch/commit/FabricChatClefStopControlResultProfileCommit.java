package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit;

//20260905_kpopmodder: Construct and atomically commit one canonical STOP result profile.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result.FabricChatClefStopControlResultFactory;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.Optional;

public final class FabricChatClefStopControlResultProfileCommit {
    private final FabricChatClefStopControlResultFactory resultFactory;

    public FabricChatClefStopControlResultProfileCommit(
            FabricChatClefStopControlResultFactory resultFactory
    ) {
        this.resultFactory = resultFactory;
    }

    public Optional<FabricChatClefCommandResultPayload> rejected(
            FabricChatClefStopControlContext context,
            String reason,
            long clientTick
    ) {
        return commit(
                context,
                resultFactory.rejected(
                        context.request().base(),
                        reason,
                        context.request().targetScope(),
                        context.request(),
                        clientTick
                )
        );
    }

    public Optional<FabricChatClefCommandResultPayload> completed(
            FabricChatClefStopControlContext context,
            long clientTick
    ) {
        return commit(context, resultFactory.completed(context, clientTick));
    }

    public Optional<FabricChatClefCommandResultPayload> unknown(
            FabricChatClefStopControlContext context,
            String reason,
            long clientTick,
            boolean mutationPossible
    ) {
        if (!mutationPossible && !context.stopCommandInvoked()) {
            context.markVerified(clientTick);
        }
        return commit(context, resultFactory.unknown(context, reason, clientTick));
    }

    private Optional<FabricChatClefCommandResultPayload> commit(
            FabricChatClefStopControlContext context,
            FabricChatClefCommandResultPayload payload
    ) {
        return context.commitResult() ? Optional.of(payload) : Optional.empty();
    }
}
