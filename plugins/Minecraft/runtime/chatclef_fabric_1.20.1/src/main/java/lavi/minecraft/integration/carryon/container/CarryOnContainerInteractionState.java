package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.integration.carryon.CarryOnObservation;

//20260805_kpopmodder: Keep per-interaction Carry On state separate from observer callbacks.
final class CarryOnContainerInteractionState {
    private final BlockInteractionContext context;
    private final CarryOnObservation stateBefore;

    CarryOnContainerInteractionState(BlockInteractionContext context, CarryOnObservation stateBefore) {
        this.context = context;
        this.stateBefore = stateBefore;
    }

    BlockInteractionContext context() {
        return context;
    }

    CarryOnObservation stateBefore() {
        return stateBefore;
    }
}
