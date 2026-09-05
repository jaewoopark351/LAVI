package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Preserve the STOP result API as a thin outcome-factory facade.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlTargetScope;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

public final class FabricChatClefStopControlResultFactory {
    private final FabricChatClefStopControlRejectionResultFactory rejectionFactory;
    private final FabricChatClefStopControlCompletedResultFactory completedFactory;
    private final FabricChatClefStopControlUnknownResultFactory unknownFactory;

    public FabricChatClefStopControlResultFactory() {
        FabricChatClefStopControlResultProfileValidator validator =
                new FabricChatClefStopControlResultProfileValidator();
        FabricChatClefStopControlResultDataFactory dataFactory =
                new FabricChatClefStopControlResultDataFactory();
        this.rejectionFactory = new FabricChatClefStopControlRejectionResultFactory(validator, dataFactory);
        this.completedFactory = new FabricChatClefStopControlCompletedResultFactory(validator, dataFactory);
        this.unknownFactory = new FabricChatClefStopControlUnknownResultFactory(validator, dataFactory);
    }

    public FabricChatClefCommandResultPayload rejected(
            FabricChatClefStopControlBaseRequest base,
            String reason,
            FabricChatClefStopControlTargetScope scope,
            FabricChatClefStopControlRequest request,
            Long verifiedClientTick
    ) {
        return rejectionFactory.create(base, reason, scope, request, verifiedClientTick);
    }

    public FabricChatClefCommandResultPayload completed(
            FabricChatClefStopControlContext context,
            long verifiedClientTick
    ) {
        return completedFactory.create(context, verifiedClientTick);
    }

    public FabricChatClefCommandResultPayload unknown(
            FabricChatClefStopControlContext context,
            String reason,
            long verifiedClientTick
    ) {
        return unknownFactory.create(context, reason, verifiedClientTick);
    }
}
