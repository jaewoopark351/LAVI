package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.speedrun.DragonBreathTracker;
import adris.altoclef.util.helpers.WorldHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;

//20260727_kpopmodder: Keeps environmental safety checks separate from FoodChain orchestration.
public class FoodSafetyPolicy {
    public boolean shouldPauseEating(AltoClef mod, DragonBreathTracker dragonBreathTracker, boolean shouldStop) {
        if (WorldHelper.isInNetherPortal()) {
            return true;
        }

        if (mod.getMobDefenseChain().isPuttingOutFire()
                || mod.getMobDefenseChain().isShielding()
                || mod.getPlayer().isBlocking()
                || mod.getMobDefenseChain().isDoingAcrobatics()) {
            return true;
        }

        dragonBreathTracker.updateBreath(mod);
        for (BlockPos playerIn : WorldHelper.getBlocksTouchingPlayer()) {
            if (dragonBreathTracker.isTouchingDragonBreath(playerIn)) {
                return true;
            }
        }

        if (!mod.getModSettings().isAutoEat()) {
            return true;
        }

        if (mod.getPlayer().isInLava()) {
            return true;
        }

        return !mod.getMLGBucketChain().doneMLG()
                || mod.getMLGBucketChain().isFalling(mod)
                || mod.getPlayer().isBlocking()
                || shouldStop;
    }

    public boolean areEnemiesNearby(AltoClef mod, boolean isTryingToEat) {
        for (Entity entity : mod.getEntityTracker().getCloseEntities()) {
            if (entity instanceof HostileEntity hostile && hostile.distanceTo(mod.getPlayer()) < (isTryingToEat ? 14 : 7)) {
                return true;
            }
        }

        return false;
    }
}
