package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.integration.carryon.CarryOnObservation;

//20260815_kpopmodder: Store one container-open postcondition window without changing interaction behavior.
final class CarryOnContainerPostconditionWindowEntry {
    private final BlockInteractionContext context;
    private final CarryOnObservation stateBefore;
    private final Object interactResult;
    private final long returnClientTickId;
    private final CarryOnContainerPostconditionSnapshot returnSnapshot;
    private int nextOffsetIndex;
    private boolean stateChangeLogged;

    CarryOnContainerPostconditionWindowEntry(BlockInteractionContext context,
                                             CarryOnObservation stateBefore,
                                             Object interactResult,
                                             long returnClientTickId,
                                             CarryOnContainerPostconditionSnapshot returnSnapshot) {
        this.context = context;
        this.stateBefore = stateBefore;
        this.interactResult = interactResult;
        this.returnClientTickId = returnClientTickId;
        this.returnSnapshot = returnSnapshot;
    }

    BlockInteractionContext context() {
        return context;
    }

    CarryOnObservation stateBefore() {
        return stateBefore;
    }

    Object interactResult() {
        return interactResult;
    }

    long returnClientTickId() {
        return returnClientTickId;
    }

    CarryOnContainerPostconditionSnapshot returnSnapshot() {
        return returnSnapshot;
    }

    int nextOffsetIndex() {
        return nextOffsetIndex;
    }

    void advanceOffsetIndex() {
        nextOffsetIndex++;
    }

    boolean stateChangeLogged() {
        return stateChangeLogged;
    }

    void markStateChangeLogged() {
        stateChangeLogged = true;
    }
}
