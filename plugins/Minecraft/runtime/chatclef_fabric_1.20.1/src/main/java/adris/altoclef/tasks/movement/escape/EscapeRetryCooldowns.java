package adris.altoclef.tasks.movement.escape;

import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//20260729_kpopmodder: Added this tracker to keep terrain escape failure cooldowns out of goal execution flow.
public class EscapeRetryCooldowns {
    private final int cooldownTicks;
    private final Map<BlockPos, Integer> cooldownUntilTicks = new HashMap<>();

    public EscapeRetryCooldowns(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public void rememberFailure(BlockPos origin) {
        if (origin != null) {
            cooldownUntilTicks.put(origin.toImmutable(), WorldHelper.getTicks() + cooldownTicks);
        }
    }

    public void pruneExpired() {
        int now = WorldHelper.getTicks();
        cooldownUntilTicks.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    public Set<BlockPos> activeOrigins() {
        pruneExpired();
        return Set.copyOf(cooldownUntilTicks.keySet());
    }

    public String describe() {
        pruneExpired();
        if (cooldownUntilTicks.isEmpty()) {
            return "none";
        }

        int now = WorldHelper.getTicks();
        List<String> entries = new ArrayList<>();
        for (Map.Entry<BlockPos, Integer> entry : cooldownUntilTicks.entrySet()) {
            entries.add(entry.getKey().toShortString() + ":" + Math.max(0, entry.getValue() - now) + "t");
        }
        return entries.toString();
    }
}
