package lavi.minecraft.integration.carryon.snapshot;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

//20260731_kpopmodder: Keep player-state reads isolated from task and Baritone snapshot code.
public final class CarryOnPlayerSnapshotCollector {
    private CarryOnPlayerSnapshotCollector() {
    }

    public static CarryOnPlayerSnapshot collect(ClientPlayerEntity player) {
        return new CarryOnPlayerSnapshot(
                playerPosition(player),
                playerVelocity(player),
                lookRotation(player),
                playerPoseState(player),
                selectedHotbarSlot(player),
                mainHandItem(player),
                offHandItem(player)
        );
    }

    private static String playerPosition(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return player.getBlockPos().toShortString();
    }

    private static String playerVelocity(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        Vec3d velocity = player.getVelocity();
        return String.format("%.3f/%.3f/%.3f", velocity.x, velocity.y, velocity.z);
    }

    private static String lookRotation(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return String.format("%.2f/%.2f", player.getYaw(), player.getPitch());
    }

    private static String playerPoseState(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return "onGround=" + player.isOnGround()
                + ",sneaking=" + player.isSneaking()
                + ",sprinting=" + player.isSprinting()
                + ",usingItem=" + player.isUsingItem()
                + ",blocking=" + player.isBlocking();
    }

    private static String selectedHotbarSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) {
            return "unavailable";
        }
        return Integer.toString(player.getInventory().selectedSlot);
    }

    private static String mainHandItem(ClientPlayerEntity player) {
        if (player == null || player.getMainHandStack() == null) {
            return "unavailable";
        }
        return String.valueOf(player.getMainHandStack().getItem());
    }

    private static String offHandItem(ClientPlayerEntity player) {
        if (player == null || player.getOffHandStack() == null) {
            return "unavailable";
        }
        return String.valueOf(player.getOffHandStack().getItem());
    }
}
