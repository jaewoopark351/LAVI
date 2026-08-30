package lavi.minecraft.diagnostics.mining.baritone.executor;

//20260830_kpopmodder: Store only permit-gated player detail values for executor events.
final class BaritonePlayerProgressSnapshot {
    final boolean present;
    final String position;
    final String blockPosition;
    final String velocity;
    final String speedSq;
    final String dimension;
    final String poseState;

    BaritonePlayerProgressSnapshot(boolean present,
                                   String position,
                                   String blockPosition,
                                   String velocity,
                                   String speedSq,
                                   String dimension,
                                   String poseState) {
        this.present = present;
        this.position = position;
        this.blockPosition = blockPosition;
        this.velocity = velocity;
        this.speedSq = speedSq;
        this.dimension = dimension;
        this.poseState = poseState;
    }
}
