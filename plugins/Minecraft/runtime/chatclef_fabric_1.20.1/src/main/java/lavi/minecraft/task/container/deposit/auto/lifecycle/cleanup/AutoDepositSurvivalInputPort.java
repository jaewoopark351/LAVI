package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import baritone.api.utils.input.Input;

//20260914_kpopmodder: Isolate native survival input access for narrow cleanup verification.
interface AutoDepositSurvivalInputPort {
    boolean shielding();
    boolean puttingOutFire();
    boolean eating();
    boolean chorusFruiting();
    boolean pressed(Input input);
    boolean forced(Input input);
    void setPressed(Input input, boolean pressed);
    void setForced(Input input, boolean forced);
}
