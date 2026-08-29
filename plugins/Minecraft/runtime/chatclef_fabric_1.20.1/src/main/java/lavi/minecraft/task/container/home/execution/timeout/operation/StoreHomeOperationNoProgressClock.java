package lavi.minecraft.task.container.home.execution.timeout.operation;

//20260828_kpopmodder: Own only the operation-wide consecutive no-progress clock.
final class StoreHomeOperationNoProgressClock {
    private int elapsedTicks;

    void tick() {
        elapsedTicks = saturatingIncrement(elapsedTicks);
    }

    void recordSemanticProgress() {
        elapsedTicks = 0;
    }

    int elapsedTicks() {
        return elapsedTicks;
    }

    private static int saturatingIncrement(int value) {
        return value == Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
    }
}
