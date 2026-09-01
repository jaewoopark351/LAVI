package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceResultDeliveryStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.terminal.CraftResourceResultSendStatus;

//20260901_kpopmodder: Separate observed send state from terminal cause and classification.
record FabricChatClefCraftResourceTerminalSendObservation(
        CraftResourceResultSendStatus sendStatus,
        CraftResourceResultDeliveryStatus deliveryStatus,
        boolean sent,
        boolean inFlight
) {
}
