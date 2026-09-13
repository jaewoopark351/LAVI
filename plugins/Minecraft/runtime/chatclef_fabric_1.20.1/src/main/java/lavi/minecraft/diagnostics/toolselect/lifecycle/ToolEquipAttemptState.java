package lavi.minecraft.diagnostics.toolselect.lifecycle;

//20260913_kpopmodder: Keep only the current synchronous equip observation; old callbacks never bind to a new attempt.
public final class ToolEquipAttemptState {
    private long currentAttempt = -1L;

    public synchronized void begin(long attemptId) {
        currentAttempt = attemptId;
    }

    public synchronized boolean isCurrent(long attemptId) {
        return attemptId >= 0L && currentAttempt == attemptId;
    }

    public synchronized void finish(long attemptId) {
        if (isCurrent(attemptId)) {
            currentAttempt = -1L;
        }
    }

    public synchronized void clear() {
        currentAttempt = -1L;
    }
}
