package lavi.minecraft.integration.carryon.snapshot;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

//20260731_kpopmodder: Keep target and crosshair reads separate from player and task snapshots.
public final class CarryOnTargetSnapshotCollector {
    private CarryOnTargetSnapshotCollector() {
    }

    public static CarryOnTargetSnapshot collect(MinecraftClient client,
                                                String targetType,
                                                String targetId,
                                                String targetPosition) {
        return new CarryOnTargetSnapshot(
                CarryOnSnapshotValues.value(targetType),
                CarryOnSnapshotValues.value(targetId),
                CarryOnSnapshotValues.value(targetPosition),
                dimension(),
                crosshairType(client),
                crosshairTarget(client),
                crosshairBlockId(client)
        );
    }

    private static String dimension() {
        try {
            return String.valueOf(WorldHelper.getCurrentDimension());
        } catch (RuntimeException e) {
            return "unavailable";
        }
    }

    private static String crosshairType(MinecraftClient client) {
        HitResult result = client == null ? null : client.crosshairTarget;
        return result == null ? "unavailable" : String.valueOf(result.getType());
    }

    private static String crosshairTarget(MinecraftClient client) {
        try {
            HitResult result = client == null ? null : client.crosshairTarget;
            if (result == null) {
                return "unavailable";
            }
            if (result instanceof BlockHitResult blockHitResult) {
                return blockHitResult.getBlockPos().toShortString() + ":" + blockHitResult.getSide();
            }
            if (result instanceof EntityHitResult entityHitResult) {
                Entity entity = entityHitResult.getEntity();
                return entity == null ? "entity:unavailable" : entity.getType() + ":" + entity.getUuidAsString();
            }
            return CarryOnSnapshotValues.value(result.getPos());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }

    private static String crosshairBlockId(MinecraftClient client) {
        try {
            if (client == null || client.world == null || !(client.crosshairTarget instanceof BlockHitResult blockHitResult)) {
                return "unavailable";
            }
            return String.valueOf(client.world.getBlockState(blockHitResult.getBlockPos()).getBlock());
        } catch (RuntimeException | LinkageError e) {
            return CarryOnSnapshotValues.unavailable(e);
        }
    }
}
