package lavi.minecraft.task.container.deposit.auto.policy;

import java.util.Objects;

public final class AutoDepositItemClassificationPolicy {
    private final AutoDepositPolicyDefinition definition;

    public AutoDepositItemClassificationPolicy(AutoDepositPolicyDefinition definition) {
        this.definition = Objects.requireNonNull(definition, "definition");
    }

    public AutoDepositItemClass classify(AutoDepositStackSnapshot stack) {
        if (!definition.loaded()) {
            return AutoDepositItemClass.UNKNOWN;
        }
        if (definition.isRareFunctional(stack.itemId())) {
            return AutoDepositItemClass.RARE_FUNCTIONAL;
        }
        if (definition.isValuable(stack.itemId())) {
            return AutoDepositItemClass.CONDITIONAL_VALUABLE;
        }
        if (!isVanilla(stack.itemId())) {
            return AutoDepositItemClass.UNKNOWN;
        }
        if (stack.food() && !definition.isHarmfulOrSpecialFood(stack.itemId())) {
            return AutoDepositItemClass.GENERAL_SURPLUS;
        }
        if (stack.role().isRecognizedEquipment()) {
            return AutoDepositItemClass.GENERAL_SURPLUS;
        }
        if (definition.isKnownGeneral(stack.itemId()) || definition.isSafeBuilding(stack.itemId())) {
            return AutoDepositItemClass.GENERAL_SURPLUS;
        }
        return AutoDepositItemClass.UNKNOWN;
    }

    public boolean isSafeBuilding(AutoDepositStackSnapshot stack) {
        return definition.isSafeBuilding(stack.itemId()) && !stack.fallingBlock();
    }

    public boolean isLog(AutoDepositStackSnapshot stack) {
        String id = stack.itemId();
        return id.endsWith("_log") || id.endsWith("_wood")
                || id.endsWith("_stem") || id.endsWith("_hyphae");
    }

    public boolean isPlank(AutoDepositStackSnapshot stack) {
        return stack.itemId().endsWith("_planks");
    }

    public boolean isFuel(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:coal") || stack.itemId().equals("minecraft:charcoal");
    }

    public boolean isSafeFood(AutoDepositStackSnapshot stack) {
        return stack.food() && !definition.isHarmfulOrSpecialFood(stack.itemId());
    }

    public boolean isTorch(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:torch");
    }

    public boolean isArrow(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:arrow")
                || stack.itemId().equals("minecraft:spectral_arrow");
    }

    public boolean isFirework(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:firework_rocket");
    }

    public boolean isWaterBucket(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:water_bucket");
    }

    public boolean isDestinationContainer(AutoDepositStackSnapshot stack) {
        return stack.itemId().equals("minecraft:chest") || stack.itemId().equals("minecraft:barrel");
    }

    private static boolean isVanilla(String itemId) {
        return itemId.startsWith("minecraft:");
    }
}
