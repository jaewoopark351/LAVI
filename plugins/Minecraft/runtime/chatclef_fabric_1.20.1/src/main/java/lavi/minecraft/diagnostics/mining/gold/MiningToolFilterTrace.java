package lavi.minecraft.diagnostics.mining.gold;

//20260913_kpopmodder: Count only candidate filters executed by the original mining policy.
public final class MiningToolFilterTrace {
    private final boolean enabled;
    private int nonPlayerSlots;
    private int wrongKind;
    private int unsuitable;
    private int savedByPolicy;
    private int durabilityRejected;
    private int accepted;

    public MiningToolFilterTrace(boolean enabled) {
        this.enabled = enabled;
    }

    public void nonPlayerSlot() { if (enabled) nonPlayerSlots++; }
    public void wrongKind() { if (enabled) wrongKind++; }
    public void unsuitable() { if (enabled) unsuitable++; }
    public void savedByPolicy() { if (enabled) savedByPolicy++; }
    public void durability(boolean acceptedValue) {
        if (enabled) {
            if (acceptedValue) accepted++;
            else durabilityRejected++;
        }
    }

    public String snapshot() {
        if (!enabled) return "NOT_CAPTURED_MODE_OFF";
        return "source=ACTUAL_POLICY_BRANCHES,nonPlayer=" + nonPlayerSlots
                + ",wrongKind=" + wrongKind + ",unsuitable=" + unsuitable
                + ",savedByPolicy=" + savedByPolicy + ",durabilityRejected=" + durabilityRejected
                + ",accepted=" + accepted + ",laterFiltersForRejectedCandidate=NOT_EVALUATED";
    }
}
