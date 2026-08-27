package lavi.minecraft.task.container.deposit.auto.trusted.interaction;

import adris.altoclef.util.Dimension;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260827_kpopmodder: Correlate one exact interaction without nearest-container or cache inference.
public final class AutoDepositOpenContainerCorrelationWindow {
    private PendingInteraction pending;

    public void record(
            Object worldIdentity,
            Dimension dimension,
            BlockPos position,
            long interactionTick) {
        if (worldIdentity == null || dimension == null || position == null) {
            pending = null;
            return;
        }
        pending = new PendingInteraction(
                worldIdentity,
                dimension,
                position.toImmutable(),
                interactionTick
        );
    }

    public Optional<BlockPos> consume(
            Object worldIdentity,
            Dimension dimension,
            long currentTick,
            long maximumAgeTicks) {
        PendingInteraction candidate = pending;
        pending = null;
        if (candidate == null
                || candidate.worldIdentity != worldIdentity
                || candidate.dimension != dimension
                || currentTick < candidate.interactionTick
                || currentTick - candidate.interactionTick > maximumAgeTicks) {
            return Optional.empty();
        }
        return Optional.of(candidate.position);
    }

    public void expire(long currentTick, long maximumAgeTicks) {
        if (pending != null
                && (currentTick < pending.interactionTick
                || currentTick - pending.interactionTick > maximumAgeTicks)) {
            pending = null;
        }
    }

    public void clear() {
        pending = null;
    }

    private static final class PendingInteraction {
        private final Object worldIdentity;
        private final Dimension dimension;
        private final BlockPos position;
        private final long interactionTick;

        private PendingInteraction(
                Object worldIdentity,
                Dimension dimension,
                BlockPos position,
                long interactionTick) {
            this.worldIdentity = worldIdentity;
            this.dimension = dimension;
            this.position = position;
            this.interactionTick = interactionTick;
        }
    }
}
