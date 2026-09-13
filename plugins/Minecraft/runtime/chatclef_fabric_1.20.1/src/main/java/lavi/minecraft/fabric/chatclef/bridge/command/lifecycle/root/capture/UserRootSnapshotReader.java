//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import net.minecraft.client.MinecraftClient;

//20260913_kpopmodder: Keep all game reads outside the pure ownership/queue state.
public final class UserRootSnapshotReader {
    public UserRootSnapshot capture() {
        try {
            if (!MinecraftClient.getInstance().isOnThread())
                return UserRootSnapshot.unavailable(UserRootReadStatus.WRONG_THREAD, "client_thread_required");
            AltoClef mod = AltoClef.getInstance();
            if (mod == null) return UserRootSnapshot.unavailable(UserRootReadStatus.ENGINE_ABSENT, "");
            UserTaskChain chain = mod.getUserTaskChain();
            if (chain == null) return UserRootSnapshot.unavailable(UserRootReadStatus.CHAIN_ABSENT, "");
            Object world = mod.getWorld();
            Object player = mod.getPlayer();
            if (world == null || player == null)
                return UserRootSnapshot.unavailable(UserRootReadStatus.WORLD_ABSENT, "");
            Task root = chain.getCurrentTask();
            return new UserRootSnapshot(root == null ? UserRootReadStatus.ROOT_ABSENT : UserRootReadStatus.PRESENT,
                    mod, chain, world, player, root, chain.currentRootLifetime(), "");
        } catch (RuntimeException error) {
            return UserRootSnapshot.unavailable(UserRootReadStatus.READ_FAILED, error.getClass().getSimpleName());
        }
    }
}
//#endif
