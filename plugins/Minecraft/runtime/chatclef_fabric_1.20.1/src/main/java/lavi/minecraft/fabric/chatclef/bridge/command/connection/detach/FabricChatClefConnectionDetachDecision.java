package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Carry one immutable detach ownership and cancellation decision.

public record FabricChatClefConnectionDetachDecision(
        String rootMatchReason,
        String boundRootOwnership,
        String cancelAction,
        boolean ownsCurrentTask
) {
}
