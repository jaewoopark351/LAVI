package lavi.minecraft.task.container.home.execution.timeout.candidate;

//20260828_kpopmodder: Count one candidate attempt's active-root ticks for observation only.
final class StoreHomeCandidateActiveTickClock {
    private int elapsedTicks;

    void tick() {
        elapsedTicks = saturatingIncrement(elapsedTicks);
    }

    int elapsedTicks() {
        return elapsedTicks;
    }

    private static int saturatingIncrement(int value) {
        return value == Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
    }
}
