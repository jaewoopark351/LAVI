package lavi.minecraft.diagnostics.container.home.timeout.progress;

import net.minecraft.util.math.BlockPos;

//20260828_kpopmodder: Carry the lightweight per-tick player/distance observation only.
public record StoreHomePlayerPositionSnapshot(
        BlockPos position,
        String positionText,
        Long distanceSquared3d,
        String captureStatus) {

    public static StoreHomePlayerPositionSnapshot unavailable() {
        return new StoreHomePlayerPositionSnapshot(
                null, "unavailable", null, "unavailable"
        );
    }
}
