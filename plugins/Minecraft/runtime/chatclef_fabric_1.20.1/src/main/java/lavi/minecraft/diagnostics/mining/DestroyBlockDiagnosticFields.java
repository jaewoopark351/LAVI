package lavi.minecraft.diagnostics.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.util.helpers.StorageHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

//20260807_kpopmodder: Keep DestroyBlock diagnostic field formatting separate from lifecycle events.
final class DestroyBlockDiagnosticFields {
    private DestroyBlockDiagnosticFields() {
    }

    static int cobblestoneCount(AltoClef mod) {
        if (mod == null) {
            return -1;
        }
        String value = ChatClefDiagnostics.safeValue(() -> mod.getItemStorage().getItemCount(Items.COBBLESTONE));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    static String targetBlockState(AltoClef mod, BlockPos target) {
        return ChatClefDiagnostics.safeValue(() -> mod == null || target == null ? "unavailable" : mod.getWorld().getBlockState(target));
    }

    static String blockStillExists(AltoClef mod, BlockPos target) {
        return ChatClefDiagnostics.safeValue(() -> mod != null && target != null && !mod.getWorld().getBlockState(target).isAir());
    }

    static String controllerBreakingBlock(AltoClef mod) {
        return ChatClefDiagnostics.safeValue(() -> mod != null && mod.getControllerExtras().isBreakingBlock());
    }

    static String breakingBlockPosition(AltoClef mod) {
        return ChatClefDiagnostics.safeValue(() -> mod == null || !mod.getControllerExtras().isBreakingBlock()
                ? "none"
                : ChatClefDiagnostics.blockPos(mod.getControllerExtras().getBreakingBlockPos()));
    }

    static String breakingProgress(AltoClef mod) {
        return ChatClefDiagnostics.safeValue(() -> mod == null ? "unavailable" : mod.getControllerExtras().getBreakingBlockProgress());
    }

    static double breakingProgressValue(AltoClef mod) {
        if (mod == null) {
            return 0.0;
        }
        try {
            return mod.getControllerExtras().getBreakingBlockProgress();
        } catch (RuntimeException | LinkageError ignored) {
            return 0.0;
        }
    }

    static Object bestToolStack(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(target))
                .map(slot -> ChatClefDiagnostics.itemStackSummary(StorageHelper.getItemStackInSlot(slot)))
                .orElse("none"));
    }

    static Object bestToolSuitable(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> StorageHelper.getBestToolSlot(mod, mod.getWorld().getBlockState(target))
                .map(slot -> StorageHelper.getItemStackInSlot(slot).getItem().getDefaultStack().isSuitableFor(mod.getWorld().getBlockState(target)))
                .orElse(false));
    }

    static Object distanceSq(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> BlockPosVer.getSquaredDistance(target, mod.getPlayer().getPos()));
    }

    static Object horizontalDistanceSq(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> {
            Vec3d playerPosition = mod.getPlayer().getPos();
            double dx = playerPosition.x - (target.getX() + 0.5);
            double dz = playerPosition.z - (target.getZ() + 0.5);
            return dx * dx + dz * dz;
        });
    }

    static Object verticalDelta(AltoClef mod, BlockPos target) {
        if (mod == null || target == null) {
            return "unavailable";
        }
        return ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getPos().y - target.getY());
    }
}
