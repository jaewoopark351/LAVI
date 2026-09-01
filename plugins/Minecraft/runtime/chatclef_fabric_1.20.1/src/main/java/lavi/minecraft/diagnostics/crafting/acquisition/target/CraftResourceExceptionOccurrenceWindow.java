package lavi.minecraft.diagnostics.crafting.acquisition.target;

//20260901_kpopmodder: Retain one bounded aggregate window per unique exception signature.
record CraftResourceExceptionOccurrenceWindow(
        long occurrenceCount,
        long firstObservedTick,
        long lastObservedTick) {
}
