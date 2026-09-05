package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.result;

//20260905_kpopmodder: Own only the immutable descriptor for one committed STOP result.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.request.validation.FabricChatClefStopControlBaseRequest;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;

import java.util.function.BooleanSupplier;

public final class FabricChatClefStopControlResultDescriptor {
    private final FabricChatClefStopControlBaseRequest request;
    private final FabricChatClefCommandResultPayload payload;
    private final boolean javaBarrierOwned;
    private final String invalidTargetFieldsMask;
    private final BooleanSupplier sentCommit;

    public FabricChatClefStopControlResultDescriptor(
            FabricChatClefStopControlBaseRequest request,
            FabricChatClefCommandResultPayload payload,
            boolean javaBarrierOwned,
            String invalidTargetFieldsMask,
            BooleanSupplier sentCommit
    ) {
        this.request = request;
        this.payload = payload;
        this.javaBarrierOwned = javaBarrierOwned;
        this.invalidTargetFieldsMask = invalidTargetFieldsMask;
        this.sentCommit = sentCommit;
    }

    public FabricChatClefStopControlBaseRequest request() {
        return request;
    }

    public FabricChatClefCommandResultPayload payload() {
        return payload;
    }

    public boolean javaBarrierOwned() {
        return javaBarrierOwned;
    }

    public String invalidTargetFieldsMask() {
        return invalidTargetFieldsMask;
    }

    public BooleanSupplier sentCommit() {
        return sentCommit;
    }
}
