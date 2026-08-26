package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.Item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public final class AutoDepositHardProtectionResult {
    private final Set<AutoDepositStackSnapshot> protectedStacks;
    private final Set<Item> protectedItems;
    private final boolean protectedRangedWeapon;
    private final boolean protectedElytra;

    AutoDepositHardProtectionResult(Set<AutoDepositStackSnapshot> protectedStacks) {
        Set<AutoDepositStackSnapshot> identityCopy = Collections.newSetFromMap(new IdentityHashMap<>());
        identityCopy.addAll(protectedStacks);
        this.protectedStacks = Collections.unmodifiableSet(identityCopy);

        Set<Item> items = new LinkedHashSet<>();
        boolean ranged = false;
        boolean elytra = false;
        for (AutoDepositStackSnapshot stack : protectedStacks) {
            items.add(stack.item());
            ranged |= stack.role().isRangedWeapon();
            elytra |= stack.role() == AutoDepositItemRole.ELYTRA;
        }
        protectedItems = Collections.unmodifiableSet(items);
        protectedRangedWeapon = ranged;
        protectedElytra = elytra;
    }

    public boolean isProtected(AutoDepositStackSnapshot stack) {
        return protectedStacks.contains(stack);
    }

    public boolean hasProtectedStackOf(Item item) {
        return protectedItems.contains(item);
    }

    public Set<AutoDepositStackSnapshot> protectedStacks() {
        return protectedStacks;
    }

    public boolean protectedRangedWeapon() {
        return protectedRangedWeapon;
    }

    public boolean protectedElytra() {
        return protectedElytra;
    }
}
