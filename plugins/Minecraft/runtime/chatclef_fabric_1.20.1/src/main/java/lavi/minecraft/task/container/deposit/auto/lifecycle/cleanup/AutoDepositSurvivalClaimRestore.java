package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import baritone.api.utils.input.Input;

//20260914_kpopmodder: Restore explicit survival input states without replaying presses, weapons, or path commands.
final class AutoDepositSurvivalClaimRestore {
    private AutoDepositSurvivalClaimRestore() { }

    static void restore(AutoDepositSurvivalInputPort inputs, AutoDepositSurvivalInputClaims claims) {
        if (claims.sneakPressed() != null) inputs.setPressed(Input.SNEAK, claims.sneakPressed());
        if (claims.usePressed() != null) inputs.setPressed(Input.CLICK_RIGHT, claims.usePressed());
        if (claims.attackForced() != null) inputs.setForced(Input.CLICK_LEFT, claims.attackForced());
    }
}
