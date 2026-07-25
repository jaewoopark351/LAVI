package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader for Baritone process state snapshots.

import adris.altoclef.AltoClef;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviBaritoneStateReader {

    private final AltoClef mod;

    public LaviBaritoneStateReader(AltoClef mod) {
        this.mod = mod;
    }

    public Map<String, Object> baritoneStatus() {
        Map<String, Object> baritone = new LinkedHashMap<>();
        if (!AltoClef.inGame()) {
            baritone.put("available", false);
            return baritone;
        }

        try {
            baritone.put("available", true);
            baritone.put("pathing", mod.getClientBaritone().getPathingBehavior().isPathing());
            baritone.put("custom_goal_active", mod.getClientBaritone().getCustomGoalProcess().isActive());
            baritone.put("builder_active", mod.getClientBaritone().getBuilderProcess().isActive());
            baritone.put("explore_active", mod.getClientBaritone().getExploreProcess().isActive());
        } catch (RuntimeException exception) {
            baritone.put("available", false);
            baritone.put("error", exception.getMessage());
        }
        return baritone;
    }
}
