//#if MC == 12001
//$$ package adris.altoclef.mixins.ownership;
//$$
//$$ import baritone.api.utils.input.Input;
//$$ import baritone.utils.InputOverrideHandler;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseLedger;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.Shadow;
//$$ import org.spongepowered.asm.mixin.Unique;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//$$ //20260914_kpopmodder: Observe native writes without cancelling, replacing, or clearing their inputs.
//$$ // FIND implementation ledger: docs/chatclef-find-implementation-contract-2026-09-14.md (input lease unit).
//$$ @Mixin(value = InputOverrideHandler.class, remap = false)
//$$ public abstract class InputOverrideLeaseMixin implements ForcedInputLeaseChannel {
//$$     @Shadow public abstract boolean isInputForcedDown(Input input);
//$$     @Shadow public abstract void setInputForceState(Input input, boolean down);
//$$     @Unique private final ForcedInputLeaseLedger lavi$inputLeases = new ForcedInputLeaseLedger();
//$$     @Unique private Thread lavi$ownedWriteThread;
//$$     @Unique private Input lavi$ownedWriteInput;
//$$
//$$     @Inject(method = "setInputForceState", at = @At("HEAD"), remap = false)
//$$     private void lavi$observeUnownedWrite(Input input, boolean down, CallbackInfo ci) {
//$$         synchronized (lavi$inputLeases) {
//$$             if (Thread.currentThread() != lavi$ownedWriteThread || input != lavi$ownedWriteInput) {
//$$                 lavi$inputLeases.externalWrite(input);
//$$             }
//$$         }
//$$     }
//$$
//$$     @Inject(method = "clearAllKeys", at = @At("HEAD"), remap = false)
//$$     private void lavi$observeUnownedClear(CallbackInfo ci) {
//$$         lavi$inputLeases.externalClear();
//$$     }
//$$
//$$     @Override public boolean lavi$claimForcedInput(Input input, Object owner) {
//$$         if (!lavi$clientThread() || input == null) return false;
//$$         synchronized (lavi$inputLeases) {
//$$             return lavi$inputLeases.claim(input, owner, isInputForcedDown(input));
//$$         }
//$$     }
//$$
//$$     @Override public boolean lavi$writeForcedInput(Input input, Object owner, boolean down) {
//$$         if (!lavi$clientThread()) return false;
//$$         synchronized (lavi$inputLeases) {
//$$             if (!lavi$inputLeases.owns(input, owner)) return false;
//$$             lavi$ownedWriteThread = Thread.currentThread();
//$$             lavi$ownedWriteInput = input;
//$$             try {
//$$                 setInputForceState(input, down);
//$$                 return true;
//$$             } finally {
//$$                 lavi$ownedWriteThread = null;
//$$                 lavi$ownedWriteInput = null;
//$$             }
//$$         }
//$$     }
//$$
//$$     @Override public boolean lavi$releaseForcedInput(Input input, Object owner) {
//$$         if (!lavi$clientThread()) return false;
//$$         synchronized (lavi$inputLeases) {
//$$             if (!lavi$writeForcedInput(input, owner, false)) return false;
//$$             return lavi$inputLeases.retire(input, owner);
//$$         }
//$$     }
//$$
//$$     @Override public boolean lavi$ownsForcedInput(Input input, Object owner) {
//$$         return lavi$inputLeases.owns(input, owner);
//$$     }
//$$
//$$     @Unique private boolean lavi$clientThread() {
//$$         MinecraftClient client = MinecraftClient.getInstance();
//$$         return client != null && client.isOnThread();
//$$     }
//$$ }
//#endif
