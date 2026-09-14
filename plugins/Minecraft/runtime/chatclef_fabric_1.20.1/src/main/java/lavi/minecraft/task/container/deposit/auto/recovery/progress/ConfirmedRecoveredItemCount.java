package lavi.minecraft.task.container.deposit.auto.recovery.progress;

//20260914_kpopmodder: Keep already-confirmed recovered-item totals independently of Task execution and diagnostics.
public final class ConfirmedRecoveredItemCount {
    private int total;

    public void add(int confirmed) {
        total = including(confirmed);
    }

    public int including(int liveConfirmed) {
        return (int) Math.min(Integer.MAX_VALUE, (long) total + Math.max(0, liveConfirmed));
    }

    public int total() { return total; }
}
