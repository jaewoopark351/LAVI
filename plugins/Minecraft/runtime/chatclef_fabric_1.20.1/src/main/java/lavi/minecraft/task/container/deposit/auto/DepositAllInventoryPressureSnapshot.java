package lavi.minecraft.task.container.deposit.auto;

//20260826_kpopmodder: Added an immutable occupied-slot snapshot for the four-fifths trigger.
public final class DepositAllInventoryPressureSnapshot {
    private static final int THRESHOLD_NUMERATOR = 4;
    private static final int THRESHOLD_DENOMINATOR = 5;

    private final int occupiedSlots;
    private final int totalSlots;

    public DepositAllInventoryPressureSnapshot(int occupiedSlots, int totalSlots) {
        if (totalSlots <= 0) {
            throw new IllegalArgumentException("totalSlots must be positive");
        }
        if (occupiedSlots < 0 || occupiedSlots > totalSlots) {
            throw new IllegalArgumentException("occupiedSlots must be between zero and totalSlots");
        }
        this.occupiedSlots = occupiedSlots;
        this.totalSlots = totalSlots;
    }

    public int occupiedSlots() {
        return occupiedSlots;
    }

    public int totalSlots() {
        return totalSlots;
    }

    public boolean isAtOrAboveThreshold() {
        return (long) occupiedSlots * THRESHOLD_DENOMINATOR
                >= (long) totalSlots * THRESHOLD_NUMERATOR;
    }
}
