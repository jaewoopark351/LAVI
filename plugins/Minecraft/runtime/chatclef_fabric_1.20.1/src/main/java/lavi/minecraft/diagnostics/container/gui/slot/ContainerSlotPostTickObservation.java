package lavi.minecraft.diagnostics.container.gui.slot;

import lavi.minecraft.diagnostics.container.gui.lifecycle.ContainerGuiFlowTrace;

//20260904_kpopmodder: Describe one later live-handler check without claiming server acknowledgement.
public record ContainerSlotPostTickObservation(
        ContainerGuiFlowTrace flow,
        ContainerSlotActionObservation action,
        ContainerItemCountSnapshot previous,
        ContainerItemCountSnapshot current,
        long observedGameTick,
        int ticksAfterRequest,
        int checkOrdinal,
        boolean changedFromRequest,
        boolean changedSincePreviousCheck,
        boolean verificationWindowClosed) {
}
