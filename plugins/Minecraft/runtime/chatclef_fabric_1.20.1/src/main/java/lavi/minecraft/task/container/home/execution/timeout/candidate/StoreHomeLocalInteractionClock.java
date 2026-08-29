package lavi.minecraft.task.container.home.execution.timeout.candidate;

//20260828_kpopmodder: Own one sticky, start-once local interaction clock with no reset path.
final class StoreHomeLocalInteractionClock {
    private boolean started;
    private int elapsedTicks;

    boolean startOnce() {
        if (started) {
            return false;
        }
        started = true;
        return true;
    }

    void tickIfStarted() {
        if (started) {
            elapsedTicks = saturatingIncrement(elapsedTicks);
        }
    }

    boolean started() {
        return started;
    }

    int elapsedTicks() {
        return elapsedTicks;
    }

    private static int saturatingIncrement(int value) {
        return value == Integer.MAX_VALUE ? Integer.MAX_VALUE : value + 1;
    }
}
