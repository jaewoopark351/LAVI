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
    private final Map<BlockPos, Integer> originCooldownUntilTicks = new HashMap<>();
    private final Map<EscapeCandidateKey, Integer> candidateCooldownUntilTicks = new HashMap<>();

    public EscapeRetryCooldowns(int cooldownTicks) {
        this.cooldownTicks = cooldownTicks;
    }

    public void rememberFailure(BlockPos origin) {
        rememberOriginFailure(origin);
    }

    public void rememberOriginFailure(BlockPos origin) {
        if (origin != null) {
            originCooldownUntilTicks.put(origin.toImmutable(), WorldHelper.getTicks() + cooldownTicks);
        }
    }

    public void rememberFailure(EscapePlan plan) {
        if (plan != null) {
            rememberCandidateFailure(plan.getCandidateKey());
        }
    }

    public void rememberCandidateFailure(EscapeCandidateKey candidateKey) {
        if (candidateKey != null) {
            candidateCooldownUntilTicks.put(candidateKey, WorldHelper.getTicks() + cooldownTicks);
        }
    }

    public void pruneExpired() {
        int now = WorldHelper.getTicks();
        originCooldownUntilTicks.entrySet().removeIf(entry -> entry.getValue() <= now);
        candidateCooldownUntilTicks.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    public Set<BlockPos> activeOrigins() {
        pruneExpired();
        return Set.copyOf(originCooldownUntilTicks.keySet());
    }

    public Set<EscapeCandidateKey> activeCandidates() {
        pruneExpired();
        return Set.copyOf(candidateCooldownUntilTicks.keySet());
    }

    public String describe() {
        pruneExpired();
        if (originCooldownUntilTicks.isEmpty() && candidateCooldownUntilTicks.isEmpty()) {
            return "none";
        }

        int now = WorldHelper.getTicks();
        return "origins=" + describeOrigins(now)
                + ", candidates=" + describeCandidates(now);
    }

    private String describeOrigins(int now) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<BlockPos, Integer> entry : originCooldownUntilTicks.entrySet()) {
            entries.add(entry.getKey().toShortString() + ":" + Math.max(0, entry.getValue() - now) + "t");
        }
        return entries.toString();
    }

    private String describeCandidates(int now) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<EscapeCandidateKey, Integer> entry : candidateCooldownUntilTicks.entrySet()) {
            entries.add(entry.getKey().describe() + ":" + Math.max(0, entry.getValue() - now) + "t");
        }
        return entries.toString();
    }
}
