package lavi.minecraft.task.container.deposit.auto.trusted;

public final class AutoDepositTrustedDestinationEvaluation {
    private final boolean eligible;
    private final int emptySlots;
    private final double distanceSquared;
    private final String state;

    public AutoDepositTrustedDestinationEvaluation(boolean eligible,
                                                   int emptySlots,
                                                   double distanceSquared,
                                                   String state) {
        this.eligible = eligible;
        this.emptySlots = emptySlots;
        this.distanceSquared = distanceSquared;
        this.state = state;
    }

    public boolean eligible() {
        return eligible;
    }

    public int emptySlots() {
        return emptySlots;
    }

    public double distanceSquared() {
        return distanceSquared;
    }

    public String state() {
        return state;
    }
}
