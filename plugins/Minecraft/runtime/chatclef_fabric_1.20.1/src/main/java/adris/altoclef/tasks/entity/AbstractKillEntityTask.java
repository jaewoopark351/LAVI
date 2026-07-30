package adris.altoclef.tasks.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.chains.MobDefenseChain;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;

import java.util.List;

/**
 * Attacks an entity, but the target entity must be specified.
 */
public abstract class AbstractKillEntityTask extends AbstractDoToEntityTask {
    private static final double OTHER_FORCE_FIELD_RANGE = 2;

    // Not the "striking" distance, but the "ok we're close enough, lower our guard for other mobs and focus on this one" range.
    private static final double CONSIDER_COMBAT_RANGE = 10;

    protected AbstractKillEntityTask() {
        this(CONSIDER_COMBAT_RANGE, OTHER_FORCE_FIELD_RANGE);
    }

    protected AbstractKillEntityTask(double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    protected AbstractKillEntityTask(double maintainDistance, double combatGuardLowerRange, double combatGuardLowerFieldRadius) {
        super(maintainDistance, combatGuardLowerRange, combatGuardLowerFieldRadius);
    }

    public static Item bestWeapon(AltoClef mod) {
        List<ItemStack> invStacks = mod.getItemStorage().getItemStacksPlayerInventory(true);

        Item bestItem = MobDefenseChain.getBestWeapon(mod);
        if (bestItem != null) {
            return bestItem;
        }

        // just get highest damage
        bestItem = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
        float bestDamage = Float.NEGATIVE_INFINITY;

        if (bestItem instanceof ToolItem handToolItem) {
            bestDamage = handToolItem.getMaterial().getAttackDamage();
        }

        for (ItemStack invStack : invStacks) {
            if (!(invStack.getItem() instanceof ToolItem item)) continue;

            float itemDamage = item.getMaterial().getAttackDamage();

            if (itemDamage > bestDamage) {
                bestItem = item;
                bestDamage = itemDamage;
            }
        }

        return bestItem;
    }

    public static boolean equipWeapon(AltoClef mod) {
        Item bestWeapon = bestWeapon(mod);
        Item equipedWeapon = StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot()).getItem();
        if (bestWeapon != null && bestWeapon != equipedWeapon) {
            mod.getSlotHandler().forceEquipItem(bestWeapon);
            return true;
        }
        return false;
    }

    @Override
    protected Task onEntityInteract(AltoClef mod, Entity entity) {
        // Equip weapon
        boolean equippedWeapon = equipWeapon(mod);
        //20260730_kpopmodder: Diagnostics-only LAVI log for entity attack boundary investigation; no behavior change.
        ChatClefDiagnostics.logEvent("ENTITY_ATTACK", "OBSERVE", "kill_entity_interact", this,
                "entity", ChatClefDiagnostics.entitySummary(entity),
                "equippedWeapon", equippedWeapon,
                "currentWeapon", ChatClefDiagnostics.itemStackSummary(StorageHelper.getItemStackInSlot(PlayerSlot.getEquipSlot())),
                "attackCooldown", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getAttackCooldownProgress(0)),
                "playerOnGround", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isOnGround()),
                "playerVelocity", ChatClefDiagnostics.safeValue(() -> ChatClefDiagnostics.vec3d(mod.getPlayer().getVelocity())),
                "touchingWater", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().isTouchingWater()));
        if (!equippedWeapon) {
            float hitProg = mod.getPlayer().getAttackCooldownProgress(0);
            if (hitProg >= 1 && (mod.getPlayer().isOnGround() || mod.getPlayer().getVelocity().getY() < 0 || mod.getPlayer().isTouchingWater())) {
                ChatClefDiagnostics.logEvent("ENTITY_ATTACK", "DECISION", "attack_entity", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "hitProgress", hitProg);
                LookHelper.lookAt(mod, entity.getEyePos());
                mod.getControllerExtras().attack(entity);
            } else {
                ChatClefDiagnostics.logEvent("ENTITY_ATTACK", "DECISION", "wait_for_attack_ready", this,
                        "entity", ChatClefDiagnostics.entitySummary(entity),
                        "hitProgress", hitProg);
            }
        } else {
            ChatClefDiagnostics.logEvent("ENTITY_ATTACK", "DECISION", "weapon_equipped_wait_next_tick", this,
                    "entity", ChatClefDiagnostics.entitySummary(entity));
        }
        return null;
    }
}
