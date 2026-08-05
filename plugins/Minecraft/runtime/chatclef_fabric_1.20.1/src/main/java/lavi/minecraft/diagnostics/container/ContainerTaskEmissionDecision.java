package lavi.minecraft.diagnostics.container;

//20260805_kpopmodder: Keep repeated container-task diagnostics bounded without changing task behavior.
final class ContainerTaskEmissionDecision {
    private final boolean emitEvent;
    private final boolean emitSummary;
    private final boolean emitCap;
    private final int suppressedRepeatCount;

    private ContainerTaskEmissionDecision(boolean emitEvent,
                                          boolean emitSummary,
                                          boolean emitCap,
                                          int suppressedRepeatCount) {
        this.emitEvent = emitEvent;
        this.emitSummary = emitSummary;
        this.emitCap = emitCap;
        this.suppressedRepeatCount = suppressedRepeatCount;
    }

    static ContainerTaskEmissionDecision event(int suppressedRepeatCount) {
        return new ContainerTaskEmissionDecision(true, false, false, suppressedRepeatCount);
    }

    static ContainerTaskEmissionDecision summary(int suppressedRepeatCount) {
        return new ContainerTaskEmissionDecision(false, true, false, suppressedRepeatCount);
    }

    static ContainerTaskEmissionDecision cap() {
        return new ContainerTaskEmissionDecision(false, false, true, 0);
    }

    static ContainerTaskEmissionDecision suppress() {
        return new ContainerTaskEmissionDecision(false, false, false, 0);
    }

    boolean emitEvent() {
        return emitEvent;
    }

    boolean emitSummary() {
        return emitSummary;
    }

    boolean emitCap() {
        return emitCap;
    }

    int suppressedRepeatCount() {
        return suppressedRepeatCount;
    }
}
