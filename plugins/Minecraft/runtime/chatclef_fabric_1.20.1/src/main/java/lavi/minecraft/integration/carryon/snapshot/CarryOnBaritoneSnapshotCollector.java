package lavi.minecraft.integration.carryon.snapshot;

import adris.altoclef.AltoClef;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

//20260731_kpopmodder: Keep Baritone/path diagnostic reads out of generic snapshot assembly.
public final class CarryOnBaritoneSnapshotCollector {
    private CarryOnBaritoneSnapshotCollector() {
    }

    public static CarryOnBaritoneSnapshot collect(AltoClef mod) {
        return new CarryOnBaritoneSnapshot(
                baritonePathing(mod),
                customGoalOwner(mod),
                breakingBlockState(mod)
        );
    }

    private static String baritonePathing(AltoClef mod) {
        try {
            if (mod == null || mod.getClientBaritone() == null) {
                return "unavailable";
            }
            return Boolean.toString(mod.getClientBaritone().getPathingBehavior().isPathing());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String customGoalOwner(AltoClef mod) {
        try {
            if (mod == null || mod.getClientBaritone() == null || mod.getClientBaritone().getCustomGoalProcess() == null) {
                return "unavailable";
            }
            return mod.getClientBaritone().getCustomGoalProcess().isActive() ? "active_owner_unavailable" : "inactive";
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String breakingBlockState(AltoClef mod) {
        try {
            if (mod == null || mod.getControllerExtras() == null || mod.getWorld() == null) {
                return "unavailable";
            }
            if (!mod.getControllerExtras().isBreakingBlock()) {
                return "false";
            }
            BlockPos pos = mod.getControllerExtras().getBreakingBlockPos();
            if (pos == null) {
                return "true@unavailable";
            }
            BlockState state = mod.getWorld().getBlockState(pos);
            return "true@" + pos.toShortString() + ":" + state.getBlock();
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }
}
