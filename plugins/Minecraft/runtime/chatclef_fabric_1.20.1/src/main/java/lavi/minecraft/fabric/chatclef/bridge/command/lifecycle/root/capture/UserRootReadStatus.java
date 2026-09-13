//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture;

//20260913_kpopmodder: Distinguish absent engine/chain/world from a successful read of no root.
public enum UserRootReadStatus {
    PRESENT, ROOT_ABSENT, ENGINE_ABSENT, CHAIN_ABSENT, WORLD_ABSENT, WRONG_THREAD, READ_FAILED
}
//#endif
