package lavi.minecraft.task.container.deposit.auto;

//20260826_kpopmodder: Added an immutable occupied-slot snapshot for automatic deposit pressure.
public final class DepositAllInventoryPressureSnapshot {
    private static final int THRESHOLD_NUMERATOR = 9;//20260826_kpopmodder : 이 값은 9로 설정되어 있으며, 이는 전체 인벤토리 수의 9/10 이상이 차지되었을 때 임계값을 나타냅니다.
    private static final int THRESHOLD_DENOMINATOR = 10;//20260826_kpopmodder : 이 값은 10으로 설정되어 있으며, 이는 전체 인벤토리 수의 9/10 이상이 차지되었을 때 임계값을 나타냅니다.
    private static final int LOW_WATER_NUMERATOR = 7;
    private static final int LOW_WATER_DENOMINATOR = 9;

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

    public int freeSlots() {
        return totalSlots - occupiedSlots;
    }

    public boolean isAtOrAboveThreshold() {
        return (long) occupiedSlots * THRESHOLD_DENOMINATOR
                >= (long) totalSlots * THRESHOLD_NUMERATOR;
    }

    public boolean isAtOrBelowLowWater() {
        return (long) occupiedSlots * LOW_WATER_DENOMINATOR
                <= (long) totalSlots * LOW_WATER_NUMERATOR;
    }

    public int targetOccupiedSlotsForRearm() {
        return (int) ((long) totalSlots * LOW_WATER_NUMERATOR / LOW_WATER_DENOMINATOR);
    }

    public int requiredReliefSlots() {
        int slotsToLowWater = Math.max(0, occupiedSlots - targetOccupiedSlotsForRearm());
        return Math.min(occupiedSlots, Math.min(8, Math.max(5, slotsToLowWater)));
    }
}
