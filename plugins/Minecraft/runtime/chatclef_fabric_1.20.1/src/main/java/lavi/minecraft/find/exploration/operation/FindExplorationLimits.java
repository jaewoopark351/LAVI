//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.operation;
//$$ 
//$$ //20260914_kpopmodder: Immutable behavioral limits are independent of diagnostic admission.
//$$ public record FindExplorationLimits(long parentNanos, long discoveryNanos, long approachNanos,
//$$         long observationNanos, long observationSliceNanos, long scanIntervalNanos, long noProgressNanos,
//$$         long maxSampleGapNanos, int localVisits, int cumulativeVisits, int scanStarts, int routeStarts,
//$$         double displacement, double sampledTravel) {
//$$     public static final FindExplorationLimits DEFAULT = new FindExplorationLimits(
//$$         300_000_000_000L, 180_000_000_000L, 120_000_000_000L,
//$$         5_000_000_000L, 50_000_000L, 1_000_000_000L, 30_000_000_000L,
//$$         5_000_000_000L, 4096, 262144, 181, 8, 512, 1024);
//$$     public FindExplorationLimits {
//$$         if (parentNanos <= 0 || discoveryNanos <= 0 || approachNanos <= 0 || observationNanos <= 0
//$$                 || observationSliceNanos <= 0 || scanIntervalNanos <= 0 || noProgressNanos <= 0
//$$                 || maxSampleGapNanos <= 0 || localVisits <= 0 || cumulativeVisits <= 0
//$$                 || scanStarts <= 0 || routeStarts <= 0 || !Double.isFinite(displacement)
//$$                 || !Double.isFinite(sampledTravel) || displacement <= 0 || sampledTravel <= 0) {
//$$             throw new IllegalArgumentException("invalid_exploration_limits");
//$$         }
//$$     }
//$$ }
//#endif

