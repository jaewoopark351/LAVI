package lavi.minecraft.task.container.deposit.auto.policy;

import net.minecraft.item.ItemStack;

public final class AutoDepositItemRoleClassifier {

    public AutoDepositItemRole classify(String itemId) {
        String path = path(itemId);
        if (path.endsWith("_pickaxe")) return AutoDepositItemRole.PICKAXE;
        if (path.endsWith("_axe")) return AutoDepositItemRole.AXE;
        if (path.endsWith("_shovel")) return AutoDepositItemRole.SHOVEL;
        if (path.endsWith("_hoe")) return AutoDepositItemRole.HOE;
        if (path.endsWith("_sword") || path.equals("mace")) return AutoDepositItemRole.MELEE_WEAPON;
        if (path.endsWith("_helmet") || path.equals("turtle_helmet")) return AutoDepositItemRole.HELMET;
        if (path.endsWith("_chestplate")) return AutoDepositItemRole.CHESTPLATE;
        if (path.endsWith("_leggings")) return AutoDepositItemRole.LEGGINGS;
        if (path.endsWith("_boots")) return AutoDepositItemRole.BOOTS;
        if (path.equals("crossbow")) return AutoDepositItemRole.CROSSBOW;
        if (path.equals("bow")) return AutoDepositItemRole.BOW;
        if (path.equals("trident")) return AutoDepositItemRole.TRIDENT;
        if (path.equals("shield")) return AutoDepositItemRole.SHIELD;
        if (path.equals("elytra")) return AutoDepositItemRole.ELYTRA;
        if (path.equals("shears") || path.equals("fishing_rod") || path.equals("flint_and_steel")
                || path.equals("brush")) {
            return AutoDepositItemRole.UTILITY_TOOL;
        }
        return AutoDepositItemRole.NONE;
    }

    public int equipmentScore(String itemId, ItemStack stack, AutoDepositItemRole role) {
        if (!role.isRecognizedEquipment()) {
            return 0;
        }
        int tier = materialTier(path(itemId));
        int durability = 10;
        if (stack.isDamageable()) {
            int maximum = Math.max(1, stack.getMaxDamage());
            durability = Math.max(0, (maximum - stack.getDamage()) * 10 / maximum);
        }
        return tier * 100 + durability;
    }

    private static int materialTier(String path) {
        if (path.startsWith("netherite_")) return 7;
        if (path.startsWith("diamond_")) return 6;
        if (path.startsWith("iron_") || path.equals("turtle_helmet")) return 5;
        if (path.startsWith("chainmail_")) return 4;
        if (path.startsWith("golden_")) return 3;
        if (path.startsWith("stone_")) return 2;
        if (path.startsWith("wooden_") || path.startsWith("leather_")) return 1;
        return 4;
    }

    private static String path(String itemId) {
        int separator = itemId.indexOf(':');
        return separator >= 0 ? itemId.substring(separator + 1) : itemId;
    }
}
