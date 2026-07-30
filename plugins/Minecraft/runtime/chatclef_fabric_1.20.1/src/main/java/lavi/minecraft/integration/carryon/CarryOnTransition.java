package lavi.minecraft.integration.carryon;

//20260730_kpopmodder: Keep Carry On success criteria in the LAVI-owned optional layer.
public enum CarryOnTransition {
    NOT_CARRYING_TO_CARRYING(CarryOnCarryState.AVAILABLE_NOT_CARRYING, CarryOnCarryState.AVAILABLE_CARRYING),
    CARRYING_TO_NOT_CARRYING(CarryOnCarryState.AVAILABLE_CARRYING, CarryOnCarryState.AVAILABLE_NOT_CARRYING),
    NONE(null, null);

    private final CarryOnCarryState expectedBefore;
    private final CarryOnCarryState expectedAfter;

    CarryOnTransition(CarryOnCarryState expectedBefore, CarryOnCarryState expectedAfter) {
        this.expectedBefore = expectedBefore;
        this.expectedAfter = expectedAfter;
    }

    public boolean matches(CarryOnObservation before, CarryOnObservation after) {
        if (this == NONE || before == null || after == null) {
            return false;
        }
        return before.state() == expectedBefore && after.state() == expectedAfter;
    }
}
