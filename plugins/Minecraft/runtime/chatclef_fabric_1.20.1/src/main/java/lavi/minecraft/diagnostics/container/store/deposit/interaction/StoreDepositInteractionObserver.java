package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
public final class StoreDepositInteractionObserver implements BlockInteractionObserver {
    @Override
    public void beforeBlockInteraction(BlockInteractionContext context,
                                       ClientPlayerEntity player,
                                       Object hand,
                                       BlockHitResult hitResult) {
        StoreDepositDiagnostics.bindInteraction(ChatClefDiagnostics.currentTaskForDiagnostics(), context);
    }

    @Override
    public void afterBlockInteraction(BlockInteractionContext context,
                                      ClientPlayerEntity player,
                                      Object hand,
                                      BlockHitResult hitResult,
                                      Object result) {
    }
}
