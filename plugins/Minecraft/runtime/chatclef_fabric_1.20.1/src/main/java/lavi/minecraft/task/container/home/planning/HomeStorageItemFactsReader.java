package lavi.minecraft.task.container.home.planning;

import adris.altoclef.multiversion.FoodComponentWrapper;
import adris.altoclef.multiversion.item.ItemVer;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRoleClassifier;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;

import java.util.Set;

//20260827_kpopmodder: Read deterministic loadout and SAFE-food facts without choosing a disposition.
public final class HomeStorageItemFactsReader {
    private static final Set<String> NON_GENERAL_FOOD = Set.of(
            "chorus_fruit",
            "enchanted_golden_apple",
            "golden_apple",
            "poisonous_potato",
            "pufferfish",
            "rotten_flesh",
            "spider_eye",
            "suspicious_stew"
    );

    private final AutoDepositItemRoleClassifier roleClassifier =
            new AutoDepositItemRoleClassifier();

    public HomeStorageItemFacts read(ItemStack stack, String itemId) {
        AutoDepositItemRole role = roleClassifier.classify(itemId);
        int maximumDurability = stack.isDamageable() ? Math.max(1, stack.getMaxDamage()) : 0;
        int remainingDurability = maximumDurability == 0
                ? 0
                : Math.max(0, maximumDurability - stack.getDamage());
        int enchantmentScore = enchantmentScore(stack);
        FoodComponentWrapper food = ItemVer.getFoodComponent(stack.getItem());
        boolean safeGeneralFood = isSafeGeneralFood(stack, itemId, food);
        int foodScore = food == null
                ? 0
                : food.getHunger() * 100 + Math.round(food.getSaturationModifier() * 10.0f);
        return new HomeStorageItemFacts(
                role,
                materialTier(path(itemId)),
                enchantmentScore,
                remainingDurability,
                maximumDurability,
                safeGeneralFood,
                foodScore
        );
    }

    private static boolean isSafeGeneralFood(
            ItemStack stack,
            String itemId,
            FoodComponentWrapper food) {
        return food != null
                && ItemVer.isFood(stack)
                && itemId.startsWith("minecraft:")
                && !NON_GENERAL_FOOD.contains(path(itemId));
    }

    private static int enchantmentScore(ItemStack stack) {
        //#if MC >= 12100
        var enchantments = stack.getEnchantments();
        return enchantments.getEnchantments().stream()
                .mapToInt(enchantments::getLevel)
                .sum();
        //#elseif MC >= 12005
        //$$ var enchantments = stack.getEnchantments();
        //$$ return enchantments.getEnchantments().stream()
        //$$         .mapToInt(entry -> enchantments.getLevel(entry.value()))
        //$$         .sum();
        //#else
        //$$ return EnchantmentHelper.get(stack).values().stream()
        //$$         .mapToInt(Integer::intValue)
        //$$         .sum();
        //#endif
    }

    private static int materialTier(String path) {
        if (path.startsWith("netherite_")) return 7;
        if (path.startsWith("diamond_")) return 6;
        if (path.startsWith("iron_")) return 5;
        if (path.startsWith("golden_")) return 3;
        if (path.startsWith("stone_")) return 2;
        if (path.startsWith("wooden_")) return 1;
        return 4;
    }

    private static String path(String itemId) {
        int separator = itemId.indexOf(':');
        return separator >= 0 ? itemId.substring(separator + 1) : itemId;
    }
}
