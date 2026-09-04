package lavi.minecraft.diagnostics.container.gui.correlation;

import lavi.minecraft.diagnostics.container.gui.runtime.ContainerGuiDiagnosticRuntime;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

//20260904_kpopmodder: Adapt the existing interaction observer seam to GUI-flow correlation only.
public final class ContainerOpenInteractionObserver implements BlockInteractionObserver {
    private final ContainerGuiDiagnosticRuntime runtime;

    public ContainerOpenInteractionObserver(ContainerGuiDiagnosticRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void beforeBlockInteraction(
            BlockInteractionContext context,
            ClientPlayerEntity player,
            Object hand,
            BlockHitResult hitResult) {
        runtime.onBlockInteractionStarted(context);
    }

    @Override
    public void afterBlockInteraction(
            BlockInteractionContext context,
            ClientPlayerEntity player,
            Object hand,
            BlockHitResult hitResult,
            Object result) {
        runtime.onBlockInteractionReturned(context, result);
    }
}
