package lavi.minecraft.diagnostics.crafting.acquisition.scope;

//20260901_kpopmodder: Retain only the bounded retirement evidence needed to reject late events.
record IronPickaxeAcquisitionTombstone(
        long retirementSequence,
        long retiredAtClientTick,
        long retiredAtMonotonicMs
) {
    boolean expired(long clientTick, long monotonicMs) {
        return elapsedAtLeast(clientTick, retiredAtClientTick, 200L)
                || elapsedAtLeast(monotonicMs, retiredAtMonotonicMs, 10_000L);
    }

    private static boolean elapsedAtLeast(long current, long start, long bound) {
        return current >= start && current - start >= bound;
    }
}
