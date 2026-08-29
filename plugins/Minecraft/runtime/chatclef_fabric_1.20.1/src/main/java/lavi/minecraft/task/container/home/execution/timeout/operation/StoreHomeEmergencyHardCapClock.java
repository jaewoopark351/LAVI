package lavi.minecraft.task.container.home.execution.timeout.operation;

//20260828_kpopmodder: Own only the non-resettable operation active-root hard-cap clock.
final class StoreHomeEmergencyHardCapClock {
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
