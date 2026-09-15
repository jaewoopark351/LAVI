package lavi.minecraft.task.container.deposit.auto.progress;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

//20260915_kpopmodder: Monotone, bounded preparation watermarks survive root replacement within one budget.
public final class AutoDepositProgressTracker {
    public static final int MAX_TARGETS = 64;
    public static final int MAX_RESOURCES = 64;
    public static final double MIN_APPROACH_BLOCKS = 1.0;

    private final Map<AutoDepositProgressSample.Target, Watermark> targets = new HashMap<>();
    private final Map<String, Watermark> resources = new HashMap<>();
    private long sequence;
    private AutoDepositProgressSample.Phase previousPhase = AutoDepositProgressSample.Phase.UNAVAILABLE;

    // One watermark implementation for a decreasing distance or increasing capped material count.
    private static final class Watermark {
        double best;
        final int materialLimit;
        long lastSeen;
        Watermark(double best, int materialLimit, long lastSeen) {
            this.best = best;
            this.materialLimit = materialLimit;
            this.lastSeen = lastSeen;
        }
    }

    public record Decision(AutoDepositProgressSample.Phase phase, boolean phaseChanged,
                           boolean navigationAdvanced, boolean resourceAdvanced,
                           boolean capacityReached, AutoDepositProgressSample.Target target,
                           double distance, double previousBestDistance,
                           int advancedResourceCount, int trackedTargets, int trackedResources,
                           String resourceKey, int resourceBestBefore, int resourceHeldAfter, int resourceLimit) {
        public boolean progressed() { return navigationAdvanced || resourceAdvanced; }
    }

    /** No credit for movement/materials acquired while another chain owned execution. */
    public void breakContinuity() { sequence++; }

    public Decision observe(AutoDepositProgressSample sample) {
        Objects.requireNonNull(sample, "sample");
        long tick = ++sequence;
        boolean phaseChanged = previousPhase != sample.phase();
        previousPhase = sample.phase();
        boolean navigationAdvanced = false;
        boolean capacityReached = false;
        double distance = Double.NaN;
        double previousBest = Double.NaN;
        AutoDepositProgressSample.Target target = null;
        if (sample.navigation().isPresent()) {
            AutoDepositProgressSample.Navigation nav = sample.navigation().orElseThrow();
            target = nav.target();
            distance = nav.distance();
            Watermark mark = targets.get(target);
            if (mark == null) {
                if (targets.size() < MAX_TARGETS) targets.put(target, new Watermark(distance, 0, tick));
                else capacityReached = true; // Never evict and re-credit an old target.
            } else {
                previousBest = mark.best;
                if (mark.lastSeen != tick - 1) {
                    // First sight after a gap/replacement is a baseline, not attributable progress.
                    mark.best = Math.min(mark.best, distance);
                } else if (distance <= mark.best - MIN_APPROACH_BLOCKS) {
                    mark.best = distance;
                    navigationAdvanced = true;
                }
                mark.lastSeen = tick;
            }
        }
        int advancedResources = 0;
        String advancedKey = "NONE";
        int resourceBestBefore = -1, resourceHeldAfter = -1, resourceLimit = -1;
        for (AutoDepositProgressSample.Resource item : sample.resources()) {
            Watermark mark = resources.get(item.key());
            if (mark == null) {
                if (resources.size() < MAX_RESOURCES) resources.put(item.key(),
                        new Watermark(Math.min(item.held(), item.needed()), item.needed(), tick));
                else capacityReached = true;
            } else if (mark.lastSeen != tick) { // Duplicate groups in one task tree cannot mint progress.
                int capped = Math.min(item.held(), Math.min(item.needed(), mark.materialLimit));
                if (mark.lastSeen == tick - 1 && capped > mark.best) {
                    advancedResources++;
                    if (advancedResources == 1) {
                        advancedKey = item.key();
                        resourceBestBefore = (int) mark.best;
                        resourceHeldAfter = capped;
                        resourceLimit = mark.materialLimit;
                    }
                }
                mark.best = Math.max(mark.best, capped);
                mark.lastSeen = tick;
            }
        }
        return new Decision(sample.phase(), phaseChanged, navigationAdvanced, advancedResources > 0,
                capacityReached, target, distance, previousBest, advancedResources, targets.size(), resources.size(),
                advancedKey, resourceBestBefore, resourceHeldAfter, resourceLimit);
    }
}
