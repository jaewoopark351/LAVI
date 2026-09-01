package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.activation;

//20260901_kpopmodder: Keep one pending and one active container identity per command scope.
final class FabricChatClefCraftResourceContainerActivationState {
    FabricChatClefCraftResourceContainerActivationCandidate pending;
    FabricChatClefCraftResourceContainerActiveTarget active;
}
