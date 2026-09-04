package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import net.minecraft.util.math.BlockPos;

//20260904_kpopmodder: Added pure validation for captured client, world, and player anchor state.
public final class AutoDepositBulkPlayerAnchorFactory {
    public AutoDepositBulkAnchorReadResult create(
            boolean clientThread,
            Object clientWorld,
            Object modWorld,
            Object clientPlayer,
            Object modPlayer,
            Object playerWorld,
            BlockPos playerPosition) {
        if (!clientThread
                || clientWorld == null
                || modWorld == null
                || clientPlayer == null
                || modPlayer == null
                || playerWorld == null
                || playerPosition == null
                || clientWorld != modWorld
                || clientPlayer != modPlayer) {
            return AutoDepositBulkAnchorReadResult.unavailable();
        }
        if (playerWorld != clientWorld) {
            return AutoDepositBulkAnchorReadResult.playerWorldMembershipMismatch();
        }
        return AutoDepositBulkAnchorReadResult.available(
                new AutoDepositBulkPlayerAnchor(
                        playerPosition,
                        clientPlayer,
                        clientWorld
                )
        );
    }
}
