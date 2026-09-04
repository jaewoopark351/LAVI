package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

//20260904_kpopmodder: Added an immutable operation-local player anchor with opaque identity bindings.
public final class AutoDepositBulkPlayerAnchor {
    private final BlockPos position;
    private final Object playerIdentity;
    private final Object capturedWorldIdentity;

    public AutoDepositBulkPlayerAnchor(
            BlockPos position,
            Object playerIdentity,
            Object capturedWorldIdentity) {
        this.position = Objects.requireNonNull(position, "position").toImmutable();
        this.playerIdentity = Objects.requireNonNull(playerIdentity, "playerIdentity");
        this.capturedWorldIdentity = Objects.requireNonNull(
                capturedWorldIdentity,
                "capturedWorldIdentity"
        );
    }

    public BlockPos position() {
        return position;
    }

    public boolean samePlayerIdentity(AutoDepositBulkPlayerAnchor other) {
        return other != null && playerIdentity == other.playerIdentity;
    }

    public boolean belongsToWorld(AutoDepositBulkWorldProvenance provenance) {
        return provenance != null && provenance.matchesWorldIdentity(capturedWorldIdentity);
    }
}
