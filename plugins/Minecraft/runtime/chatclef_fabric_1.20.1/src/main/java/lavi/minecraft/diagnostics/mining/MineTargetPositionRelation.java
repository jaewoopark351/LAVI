package lavi.minecraft.diagnostics.mining;

import net.minecraft.util.math.BlockPos;

//20260809_kpopmodder: Classify mining target coordinate changes for target-level retry diagnostics.
public final class MineTargetPositionRelation {
    private MineTargetPositionRelation() {
    }

    public static String classify(BlockPos previous, BlockPos candidate) {
        if (candidate == null) {
            return "NO_CANDIDATE";
        }
        if (previous == null) {
            return "NO_PREVIOUS_TARGET";
        }
        int dx = Math.abs(candidate.getX() - previous.getX());
        int dy = Math.abs(candidate.getY() - previous.getY());
        int dz = Math.abs(candidate.getZ() - previous.getZ());
        if (dx == 0 && dy == 0 && dz == 0) {
            return "SAME_COORDINATE";
        }
        if (dx + dy + dz == 1) {
            return "ADJACENT_FACE";
        }
        if (Math.max(dx, Math.max(dy, dz)) <= 1) {
            return "ADJACENT_EDGE_OR_CORNER";
        }
        if (Math.max(dx, Math.max(dy, dz)) <= 4) {
            return "NEARBY_CLUSTER";
        }
        return "DIFFERENT_REGION";
    }

    public static Object manhattanDistance(BlockPos previous, BlockPos candidate) {
        if (previous == null || candidate == null) {
            return "unavailable";
        }
        return Math.abs(candidate.getX() - previous.getX())
                + Math.abs(candidate.getY() - previous.getY())
                + Math.abs(candidate.getZ() - previous.getZ());
    }

    public static Object chebyshevDistance(BlockPos previous, BlockPos candidate) {
        if (previous == null || candidate == null) {
            return "unavailable";
        }
        int dx = Math.abs(candidate.getX() - previous.getX());
        int dy = Math.abs(candidate.getY() - previous.getY());
        int dz = Math.abs(candidate.getZ() - previous.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }

    public static Object squaredDistance(BlockPos previous, BlockPos candidate) {
        if (previous == null || candidate == null) {
            return "unavailable";
        }
        long dx = candidate.getX() - previous.getX();
        long dy = candidate.getY() - previous.getY();
        long dz = candidate.getZ() - previous.getZ();
        return dx * dx + dy * dy + dz * dz;
    }
}
