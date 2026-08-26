package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import net.minecraft.util.math.BlockPos;

public record StoreContainerRangeTransition(StoreContainerRangeSnapshot current,
                                            boolean rawPreviousAvailable,
                                            boolean rawWithin50Before,
                                            String rawRangeCrossing,
                                            boolean currentTryPreviousAvailable,
                                            boolean currentTryWithin70Before,
                                            String currentTryRangeCrossing,
                                            String selectedBranch,
                                            long branchEpoch,
                                            boolean branchChanged) {
    public static StoreContainerRangeTransition unavailable() {
        return new StoreContainerRangeTransition(
                StoreContainerRangeSnapshot.unavailable(),
                false,
                false,
                "NONE",
                false,
                false,
                "NONE",
                "NONE",
                0,
                false
        );
    }

    public boolean hasCrossing() {
        return !"NONE".equals(rawRangeCrossing) || !"NONE".equals(currentTryRangeCrossing);
    }

    public String sample() {
        if (!hasCrossing()) {
            return "none";
        }
        return "branch=" + selectedBranch
                + ",epoch=" + branchEpoch
                + ",player=" + playerPosition()
                + ",rawTarget=" + position(current.rawTarget())
                + ",rawDistanceSquared=" + value(current.rawDistanceSquared())
                + ",rawCrossing=" + rawRangeCrossing
                + ",currentTryTarget=" + position(current.currentTryTarget())
                + ",currentTryDistanceSquared=" + value(current.currentTryDistanceSquared())
                + ",currentTryCrossing=" + currentTryRangeCrossing;
    }

    private String playerPosition() {
        return current.playerPosition() == null
                ? "unavailable"
                : current.playerPosition().getX()
                        + "/" + current.playerPosition().getY()
                        + "/" + current.playerPosition().getZ();
    }

    private static String value(Double number) {
        return number == null ? "unavailable" : number.toString();
    }

    private static String position(BlockPos position) {
        return position == null ? "none" : position.toShortString();
    }
}
