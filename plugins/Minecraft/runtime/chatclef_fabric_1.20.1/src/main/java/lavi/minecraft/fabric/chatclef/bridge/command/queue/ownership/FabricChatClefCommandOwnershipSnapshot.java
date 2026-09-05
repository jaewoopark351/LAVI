package lavi.minecraft.fabric.chatclef.bridge.command.queue.ownership;

//20260905_kpopmodder: Represent one atomic view of ordinary pending and active ownership.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

public record FabricChatClefCommandOwnershipSnapshot(
        FabricChatClefCommandContext active,
        FabricChatClefCommandContext pending
) {
}
