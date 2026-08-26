package lavi.minecraft.task.container.deposit.auto.policy;

import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AutoDepositHardProtectionPolicy {
    private final AutoDepositPolicyDefinition definition;

    public AutoDepositHardProtectionPolicy(AutoDepositPolicyDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public AutoDepositHardProtectionResult evaluate(List<AutoDepositStackSnapshot> stacks) {
        Objects.requireNonNull(stacks, "stacks");
        Set<AutoDepositStackSnapshot> protectedStacks = Collections.newSetFromMap(new IdentityHashMap<>());
        Map<AutoDepositItemRole, AutoDepositStackSnapshot> primaries =
                new EnumMap<>(AutoDepositItemRole.class);

        for (AutoDepositStackSnapshot stack : stacks) {
            if (isDirectlyProtected(stack)) {
                protectedStacks.add(stack);
            }
            if (stack.role().isPrimaryToolOrWeapon() || stack.role().isArmor()) {
                AutoDepositStackSnapshot current = primaries.get(stack.role());
                if (current == null || betterPrimary(stack, current)) {
                    primaries.put(stack.role(), stack);
                }
            }
        }
        protectedStacks.addAll(primaries.values());
        return new AutoDepositHardProtectionResult(protectedStacks);
    }

    private boolean isDirectlyProtected(AutoDepositStackSnapshot stack) {
        return stack.isEquipped()
                || stack.selectedMainHand()
                || stack.botProtected()
                || stack.specialMetadata()
                || definition.isAlwaysHard(stack.itemId());
    }

    private static boolean betterPrimary(AutoDepositStackSnapshot candidate,
                                         AutoDepositStackSnapshot current) {
        if (candidate.equipmentScore() != current.equipmentScore()) {
            return candidate.equipmentScore() > current.equipmentScore();
        }
        if (candidate.isEquipped() != current.isEquipped()) {
            return candidate.isEquipped();
        }
        return candidate.slotIndex() < current.slotIndex();
    }
}
