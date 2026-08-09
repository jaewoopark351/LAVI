package lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session;

import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.detach.FabricChatClefCommandOwnershipDetachedFlagPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.ownership.payload.session.detach.FabricChatClefCommandOwnershipDetachedReasonPayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep command ownership detach fields separate at the same Map edge.
public final class FabricChatClefCommandOwnershipDetachPayloadMap {
    private FabricChatClefCommandOwnershipDetachPayloadMap() {
    }

    public static void putDetachFields(
            Map<String, Object> payload,
            boolean detached,
            String detachedReason
    ) {
        FabricChatClefCommandOwnershipDetachedFlagPayloadMap.writeTo(payload, detached);
        FabricChatClefCommandOwnershipDetachedReasonPayloadMap.writeTo(payload, detachedReason);
    }
}
