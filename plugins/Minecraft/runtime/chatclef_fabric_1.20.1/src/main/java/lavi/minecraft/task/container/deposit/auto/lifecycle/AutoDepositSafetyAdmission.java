package lavi.minecraft.task.container.deposit.auto.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.MobDefenseChain;

//20260914_kpopmodder: Read native cached survival decisions without another priority evaluation.
public final class AutoDepositSafetyAdmission {
    private AutoDepositSafetyAdmission() { }

    public static boolean claimed(AltoClef mod) {
        MobDefenseChain defense = mod.getMobDefenseChain();
        //#if MC == 12001
        if (defense != null && (defense.isToolInputClaimed() || defense.isPuttingOutFire())) return true;
        //#else
        //$$ if (defense != null && (defense.isShielding() || defense.isPuttingOutFire())) return true;
        //#endif
        return (mod.getFoodChain() != null && mod.getFoodChain().isTryingToEat())
                || (mod.getMLGBucketChain() != null && mod.getMLGBucketChain().isChorusFruiting());
    }
}
