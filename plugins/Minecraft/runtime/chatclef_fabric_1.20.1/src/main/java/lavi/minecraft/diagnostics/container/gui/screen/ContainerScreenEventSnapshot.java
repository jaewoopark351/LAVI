package lavi.minecraft.diagnostics.container.gui.screen;

import lavi.minecraft.diagnostics.container.gui.correlation.ContainerOpenInteractionObservation;
import lavi.minecraft.diagnostics.container.gui.correlation.ContainerTargetFamily;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;

//20260904_kpopmodder: Preserve one immutable screen-event observation without retaining the mutable event.
public record ContainerScreenEventSnapshot(
        String screenOpenEventIdentity,
        boolean eventPreOpen,
        String screenObjectIdentity,
        String screenName,
        String screenTypeActual,
        boolean screenIsHandled,
        String handledScreenHandlerIdentity,
        String playerHandlerIdentity,
        int capturedSyncId,
        int liveSyncId,
        String handlerTypeActual,
        boolean eventScreenIsCurrentScreen,
        boolean eventHandlerIsPlayerHandler,
        ContainerTargetFamily targetFamily,
        String target,
        String targetBlockId,
        String liveBlockId,
        String worldIdentity,
        String dimension,
        String screenTypeExpected,
        String handlerTypeExpected,
        boolean screenTypeMatched,
        boolean handlerTypeMatched,
        boolean diagnosticCorrelationCandidateAccepted,
        String diagnosticCorrelationReason,
        ContainerOpenInteractionObservation interaction,
        ContainerClientTickWindowSnapshot tickWindow) {
}
