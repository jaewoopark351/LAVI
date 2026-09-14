//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.operation;
//$$ 
//$$ import lavi.minecraft.find.observation.FindObservationPort.Binding;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ 
//$$ //20260914_kpopmodder: Tick sampling survives preemption and measures travel separately from route progress.
//$$ public final class FindDiscoveryProgress {
//$$     private final Binding origin;
//$$     private final long maxGap;
//$$     private final FindLog log;
//$$     private double x, z, travel, displacement;
//$$     private long sampledAt, activeWithoutProgress;
//$$     private boolean active;
//$$     public FindDiscoveryProgress(Binding origin, long now, long maxGap) {
//$$         this(origin, now, maxGap, null);
//$$     }
//$$     public FindDiscoveryProgress(Binding origin, long now, long maxGap, FindLog log) {
//$$         this.origin = origin; this.maxGap = maxGap; this.log = log; x = origin.x(); z = origin.z(); sampledAt = now;
//$$         requirePosition(origin);
//$$     }
//$$     public void setActive(boolean active, long now) {
//$$         boolean before = this.active; this.active = active; sampledAt = now;
//$$         if (log != null && before != active) log.event("DISCOVERY_PROGRESS_CLOCK_CHANGED", "reason", active ? "active_exploration" : "inactive",
//$$             "activeBefore", before, "activeAfter", active, "noProgressAfter", activeWithoutProgress, "budgetPreserved", true);
//$$     }
//$$     public void sample(Binding current, long now) {
//$$         requirePosition(current);
//$$         long delta = now - sampledAt;
//$$         if (delta < 0 || delta > maxGap) throw new IllegalStateException("movement_sample_unavailable");
//$$         double segment = Math.hypot(current.x() - x, current.z() - z);
//$$         double previousTravel = travel; long previousNoProgress = activeWithoutProgress;
//$$         double next = travel + segment;
//$$         if (!Double.isFinite(segment) || !Double.isFinite(next)) throw new IllegalStateException("movement_sample_unavailable");
//$$         travel = next; displacement = Math.hypot(current.x() - origin.x(), current.z() - origin.z());
//$$         if (active) activeWithoutProgress += delta;
//$$         x = current.x(); z = current.z(); sampledAt = now;
//$$         if (log != null) log.event("DISCOVERY_MOVEMENT_SAMPLED", "reason", segment == 0 ? "stationary" : "sampledmotion",
//$$             "travelBefore", previousTravel, "travelDelta", segment, "travelAfter", travel, "displacement", displacement,
//$$             "noProgressBefore", previousNoProgress, "noProgressDelta", activeWithoutProgress - previousNoProgress,
//$$             "noProgressAfter", activeWithoutProgress, "active", active);
//$$     }
//$$     public void verifiedProgress() {
//$$         long before = activeWithoutProgress; activeWithoutProgress = 0;
//$$         if (log != null) log.event("DISCOVERY_WAYPOINT_PROGRESS", "reason", "full_waypoint_arrival",
//$$             "noProgressBefore", before, "noProgressDelta", -before, "noProgressAfter", 0);
//$$     }
//$$     public double travel() { return travel; }
//$$     public double displacement() { return displacement; }
//$$     public long activeWithoutProgress() { return activeWithoutProgress; }
//$$     public long projectedActiveWithoutProgress(long now) {
//$$         return activeWithoutProgress + (active ? Math.max(0, now - sampledAt) : 0);
//$$     }
//$$     private static void requirePosition(Binding value) {
//$$         if (value == null || !Double.isFinite(value.x()) || !Double.isFinite(value.y()) || !Double.isFinite(value.z())) {
//$$             throw new IllegalStateException("movement_sample_unavailable");
//$$         }
//$$     }
//$$ }
//#endif
