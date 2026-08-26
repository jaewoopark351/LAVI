package lavi.minecraft.task.container.deposit.auto.recovery;

import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public record AutoDepositRecoveryCandidate(BlockPos position,
                                           Tier tier,
                                           Map<Item, Integer> withdrawalLimits) {
    public AutoDepositRecoveryCandidate {
        position = Objects.requireNonNull(position, "position").toImmutable();
        tier = Objects.requireNonNull(tier, "tier");
        withdrawalLimits = Collections.unmodifiableMap(new LinkedHashMap<>(withdrawalLimits));
    }

    public enum Tier {
        CONFIRMED_DESTINATION,
        CURRENT_DIMENSION_CACHE,
        NEARBY_SCANNER
    }
}
