package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

//20260907_kpopmodder: Separate Minecraft inventory reads from GET effect bookkeeping.
@FunctionalInterface
interface FabricChatClefGetItemTargetCountReader {
    FabricChatClefGetItemCountObservation read();
}
