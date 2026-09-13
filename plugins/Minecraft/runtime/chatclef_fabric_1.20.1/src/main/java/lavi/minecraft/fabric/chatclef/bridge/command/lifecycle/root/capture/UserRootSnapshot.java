//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture;

import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;

//20260913_kpopmodder: Typed authoritative user-root read; selected chains and diagnostic IDs are not ownership.
public record UserRootSnapshot(UserRootReadStatus status, Object engine, Object chain, Object world,
                               Object player, Object root, UserRootLifetime lifetime, String error) {
    public boolean validLifetime() { return status == UserRootReadStatus.PRESENT || status == UserRootReadStatus.ROOT_ABSENT; }
    public boolean sameEnvironment(UserRootSnapshot other) {
        return other != null && validLifetime() && other.validLifetime()
                && engine == other.engine && chain == other.chain && world == other.world && player == other.player;
    }
    public static UserRootSnapshot unavailable(UserRootReadStatus status, String error) {
        return new UserRootSnapshot(status, null, null, null, null, null, null, error);
    }
}
//#endif
