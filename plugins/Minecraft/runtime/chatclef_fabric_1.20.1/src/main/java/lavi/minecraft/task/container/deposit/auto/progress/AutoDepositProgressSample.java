package lavi.minecraft.task.container.deposit.auto.progress;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260915_kpopmodder: Immutable preparation facts, separate from confirmed storage/recovery effects.
public record AutoDepositProgressSample(Phase phase, Optional<Navigation> navigation, List<Resource> resources) {
    public enum Phase { UNAVAILABLE, PREPARE_CONTAINER, APPROACH_CONTAINER }
    public static final int MAX_RESOURCE_SAMPLES = 16;
    public static final int MAX_RESOURCE_KEY_LENGTH = 4096;
    public static final AutoDepositProgressSample UNAVAILABLE =
            new AutoDepositProgressSample(Phase.UNAVAILABLE, Optional.empty(), List.of());

    public AutoDepositProgressSample {
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(navigation, "navigation");
        resources = List.copyOf(resources);
        if (phase == Phase.UNAVAILABLE && (navigation.isPresent() || !resources.isEmpty())) {
            throw new IllegalArgumentException("Unavailable phase cannot carry progress evidence");
        }
        if (phase == Phase.APPROACH_CONTAINER && !resources.isEmpty()) {
            throw new IllegalArgumentException("Preparation resources require the preparation phase");
        }
        if (resources.size() > MAX_RESOURCE_SAMPLES) throw new IllegalArgumentException("Too many resource samples");
    }

    /** The owner's block position, not a goal object's identity, radius, debug text, or path generation. */
    public record Target(int x, int y, int z) { }

    public record Navigation(Target target, double distance) {
        public Navigation {
            Objects.requireNonNull(target, "target");
            if (!Double.isFinite(distance) || distance < 0) throw new IllegalArgumentException("Invalid distance");
        }
    }

    /** Canonical registry-ID set of one current ResourceTask output; held is main inventory only. */
    public record Resource(String key, int held, int needed) {
        public Resource {
            Objects.requireNonNull(key, "key");
            if (key.isBlank() || key.length() > MAX_RESOURCE_KEY_LENGTH || held < 0 || needed <= 0) {
                throw new IllegalArgumentException("Invalid resource progress fact");
            }
        }
    }
}
