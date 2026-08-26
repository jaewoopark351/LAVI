package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

public record StoreContainerRangeAggregate(int raw50CrossingCount,
                                           int currentTry70CrossingCount,
                                           int branchChangeAtRangeCrossingCount,
                                           int resourceChildInterruptedAtRangeCrossingCount,
                                           String firstRangeCrossingSample,
                                           String lastRangeCrossingSample) {
    public static StoreContainerRangeAggregate empty() {
        return new StoreContainerRangeAggregate(0, 0, 0, 0, "none", "none");
    }
}
