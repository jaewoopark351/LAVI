package lavi.minecraft.task.container.deposit.auto.policy;

public enum AutoDepositItemRole {
    NONE,
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,
    MELEE_WEAPON,
    HELMET,
    CHESTPLATE,
    LEGGINGS,
    BOOTS,
    BOW,
    CROSSBOW,
    TRIDENT,
    SHIELD,
    ELYTRA,
    UTILITY_TOOL;

    public boolean isPrimaryToolOrWeapon() {
        return this == PICKAXE || this == AXE || this == SHOVEL || this == MELEE_WEAPON;
    }

    public boolean isArmor() {
        return this == HELMET || this == CHESTPLATE || this == LEGGINGS || this == BOOTS;
    }

    public boolean isRangedWeapon() {
        return this == BOW || this == CROSSBOW;
    }

    public boolean isRecognizedEquipment() {
        return this != NONE;
    }
}
