package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

import adris.altoclef.AltoClef;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

//20260914_kpopmodder: Adapt existing passive survival flags without evaluating chain priority again.
final class MinecraftAutoDepositSurvivalInputPort implements AutoDepositSurvivalInputPort {
    private final AltoClef mod;

    MinecraftAutoDepositSurvivalInputPort(AltoClef mod) {
        this.mod = mod;
    }

    private boolean available() {
        return mod != null && mod.getPlayer() != null && mod.getInputControls() != null;
    }

    @Override public boolean shielding() {
        return available() && mod.getMobDefenseChain() != null && mod.getMobDefenseChain().isShielding();
    }

    @Override public boolean puttingOutFire() {
        return available() && mod.getClientBaritone() != null && mod.getMobDefenseChain() != null
                && mod.getMobDefenseChain().isPuttingOutFire();
    }

    @Override public boolean eating() {
        return available() && mod.getFoodChain() != null && mod.getFoodChain().isTryingToEat();
    }

    @Override public boolean chorusFruiting() {
        return available() && mod.getMLGBucketChain() != null && mod.getMLGBucketChain().isChorusFruiting();
    }

    @Override public boolean pressed(Input input) {
        return mod.getInputControls().isHeldDown(input);
    }

    @Override public boolean forced(Input input) {
        return mod.getClientBaritone().getInputOverrideHandler().isInputForcedDown(input);
    }

    @Override public void setPressed(Input input, boolean pressed) {
        // Restore the existing state only: InputControls.hold/tryPress could enqueue another key press.
        key(input).setPressed(pressed);
    }

    @Override public void setForced(Input input, boolean forced) {
        mod.getClientBaritone().getInputOverrideHandler().setInputForceState(input, forced);
    }

    private KeyBinding key(Input input) {
        return switch (input) {
            case SNEAK -> MinecraftClient.getInstance().options.sneakKey;
            case CLICK_RIGHT -> MinecraftClient.getInstance().options.useKey;
            default -> throw new IllegalArgumentException("Not an automatic-deposit survival key: " + input);
        };
    }
}
