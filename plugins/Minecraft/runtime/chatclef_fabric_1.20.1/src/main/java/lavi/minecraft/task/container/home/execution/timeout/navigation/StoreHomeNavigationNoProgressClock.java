package lavi.minecraft.task.container.home.execution.timeout.navigation;

//20260828_kpopmodder: Count consecutive active navigation ticks without accepted semantic progress.
public final class StoreHomeNavigationNoProgressClock {
    private int elapsedTicks;

    public void tick(boolean semanticProgress) {
        elapsedTicks = semanticProgress
                ? 0
                : saturatingIncrement(elapsedTicks);
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    private static int saturatingIncrement(int value) {
        return value == Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
    }
}
