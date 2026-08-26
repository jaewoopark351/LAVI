package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

public final class AutoDepositCategoryReservePolicy {
    private final AutoDepositPolicyDefinition definition;
    private final AutoDepositItemClassificationPolicy classification;

    public AutoDepositCategoryReservePolicy(AutoDepositPolicyDefinition definition,
                                            AutoDepositItemClassificationPolicy classification) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.classification = Objects.requireNonNull(classification, "classification");
    }

    public Map<Item, Integer> allocate(List<AutoDepositStackSnapshot> stacks,
                                       AutoDepositHardProtectionResult hardProtection,
                                       boolean destinationResourceNeeded) {
        Objects.requireNonNull(stacks, "stacks");
        Objects.requireNonNull(hardProtection, "hardProtection");
        List<AutoDepositStackSnapshot> candidates = stacks.stream()
                .filter(AutoDepositStackSnapshot::isMainInventory)
                .filter(stack -> !hardProtection.hasProtectedStackOf(stack.item()))
                .toList();
        Map<Item, Integer> result = new LinkedHashMap<>();

        allocateCategory(result, candidates, classification::isSafeBuilding, definition.safeBuildingReserve());
        boolean logsPresent = candidates.stream().anyMatch(classification::isLog);
        if (logsPresent) {
            allocateCategory(result, candidates, classification::isLog, definition.logReserve());
        } else {
            allocateCategory(result, candidates, classification::isPlank, definition.plankFallbackReserve());
        }
        allocateCategory(result, candidates, classification::isFuel, definition.fuelReserve());
        allocateCategory(result, candidates, classification::isSafeFood, definition.foodReserve());
        allocateCategory(result, candidates, classification::isTorch, definition.torchReserve());
        allocateCategory(result, candidates, classification::isWaterBucket, definition.waterBucketReserve());
        if (hardProtection.protectedRangedWeapon()) {
            allocateCategory(result, candidates, classification::isArrow, definition.arrowReserve());
        }
        if (hardProtection.protectedElytra()) {
            allocateCategory(result, candidates, classification::isFirework, definition.fireworkReserve());
        }
        if (destinationResourceNeeded) {
            allocateCategory(result, candidates, classification::isDestinationContainer,
                    definition.destinationContainerReserve());
        }
        return Collections.unmodifiableMap(result);
    }

    private static void allocateCategory(Map<Item, Integer> result,
                                         List<AutoDepositStackSnapshot> stacks,
                                         Predicate<AutoDepositStackSnapshot> category,
                                         int reserve) {
        if (reserve <= 0) {
            return;
        }
        Map<Item, Choice> choices = new LinkedHashMap<>();
        for (AutoDepositStackSnapshot stack : stacks) {
            if (!category.test(stack)) {
                continue;
            }
            Choice choice = choices.computeIfAbsent(
                    stack.item(), ignored -> new Choice(stack.item(), stack.itemId())
            );
            choice.count += stack.count();
        }
        List<Choice> ordered = new ArrayList<>(choices.values());
        ordered.sort(Comparator.comparingInt(Choice::count).reversed().thenComparing(Choice::itemId));

        int remaining = reserve;
        for (Choice choice : ordered) {
            int allocation = Math.min(remaining, choice.count());
            if (allocation > 0) {
                result.merge(choice.item(), allocation, Math::max);
                remaining -= allocation;
            }
            if (remaining == 0) {
                break;
            }
        }
    }

    private static final class Choice {
        private final Item item;
        private final String itemId;
        private int count;

        private Choice(Item item, String itemId) {
            this.item = item;
            this.itemId = itemId;
        }

        private Item item() {
            return item;
        }

        private String itemId() {
            return itemId;
        }

        private int count() {
            return count;
        }
    }
}
