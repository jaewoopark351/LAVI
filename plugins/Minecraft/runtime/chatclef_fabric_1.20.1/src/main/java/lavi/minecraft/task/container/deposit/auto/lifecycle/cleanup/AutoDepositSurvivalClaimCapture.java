package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import baritone.api.utils.input.Input;

//20260914_kpopmodder: Capture only current semantic survival claims, never all held keys or old storage state.
final class AutoDepositSurvivalClaimCapture {
    private AutoDepositSurvivalClaimCapture() { }

    static AutoDepositSurvivalInputClaims capture(AutoDepositSurvivalInputPort inputs) {
        boolean shield = inputs.shielding();
        boolean use = shield || inputs.eating() || inputs.chorusFruiting();
        boolean fire = inputs.puttingOutFire();
        return new AutoDepositSurvivalInputClaims(
                shield ? inputs.pressed(Input.SNEAK) : null,
                use ? inputs.pressed(Input.CLICK_RIGHT) : null,
                fire ? inputs.forced(Input.CLICK_LEFT) : null);
    }
}
