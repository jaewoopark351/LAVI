package adris.altoclef.tasks.construction.carryon;

import java.util.Collections;
import java.util.List;

//20260728_kpopmodder: Added this helper to keep Carry On placement candidate cycling separate from task execution.
public final class CarryOnPlacementCandidateCycle {

    private List<CarriedBlockPlacementPlanner.PlacementTarget> targets = Collections.emptyList();
    private int index;
    private boolean blockEmptyFallback;

    public void reset() {
        targets = Collections.emptyList();
        index = 0;
        blockEmptyFallback = false;
    }

    public boolean setStrict(CarriedBlockPlacementPlanner.PlacementTarget target) {
        if (target == null) {
            reset();
            return false;
        }
        targets = List.of(target);
        index = 0;
        blockEmptyFallback = false;
        return true;
    }

    public boolean setBlockEmptyFallback(List<CarriedBlockPlacementPlanner.PlacementTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            reset();
            return false;
        }
        this.targets = List.copyOf(targets);
        index = 0;
        blockEmptyFallback = true;
        return true;
    }

    public CarriedBlockPlacementPlanner.PlacementTarget current() {
        if (targets.isEmpty()) {
            return null;
        }
        if (index >= targets.size()) {
            index = 0;
        }
        return targets.get(index);
    }

    public void advance() {
        if (targets.size() <= 1) {
            return;
        }
        index = (index + 1) % targets.size();
    }

    public boolean isBlockEmptyFallback() {
        return blockEmptyFallback;
    }

    public int size() {
        return targets.size();
    }

    public String mode() {
        return blockEmptyFallback ? "block-empty-cycle" : "strict";
    }
}
