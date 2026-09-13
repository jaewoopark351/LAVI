//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.binding;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.GotoTarget;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.movement.gotoresult.model.GotoTargetSnapshot;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import java.util.Locale;

//20260913_kpopmodder: Retain the admitted world/player independently of later Idle or root assignment.
public final class GotoTaskBinding {
    private final ClientWorld world;
    private final ClientPlayerEntity player;
    private final GotoTargetSnapshot target;

    public GotoTaskBinding(AltoClef mod, GotoTarget request) {
        world = mod.getWorld();
        player = mod.getPlayer();
        target = new GotoTargetSnapshot(request.getX(), request.getY(), request.getZ(),
                request.getDimension() == null ? null : request.getDimension().name().toLowerCase(Locale.ROOT),
                dimension(mod));
    }

    public GotoTargetSnapshot target() { return target; }

    public boolean worldMatches(AltoClef mod) {
        return mod != null && world != null && player != null && mod.getWorld() == world
                && mod.getPlayer() == player && player.isAlive();
    }

    public boolean rootMatches(AltoClef mod, Task root) {
        return worldMatches(mod) && mod.getUserTaskChain().getCurrentTask() == root;
    }

    public static String dimension(AltoClef mod) {
        return mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue().toString();
    }
}
//#endif
