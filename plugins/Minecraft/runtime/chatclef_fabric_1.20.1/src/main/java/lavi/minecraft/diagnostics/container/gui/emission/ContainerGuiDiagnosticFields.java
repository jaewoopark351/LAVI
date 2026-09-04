package lavi.minecraft.diagnostics.container.gui.emission;

import lavi.minecraft.diagnostics.container.gui.budget.ContainerGuiDiagnosticAggregateSnapshot;
import lavi.minecraft.diagnostics.container.gui.screen.ContainerScreenEventSnapshot;
import lavi.minecraft.diagnostics.container.gui.tick.ContainerClientTickWindowSnapshot;

//20260904_kpopmodder: Project canonical field names separately from observation and emission ownership.
public final class ContainerGuiDiagnosticFields {
    private ContainerGuiDiagnosticFields() {
    }

    public static Object[] screen(
            ContainerScreenEventSnapshot snapshot,
            String screenEventStage,
            String screenEventDisposition,
            String screenEventDropReason) {
        return screen(
                snapshot,
                snapshot.tickWindow(),
                screenEventStage,
                screenEventDisposition,
                screenEventDropReason
        );
    }

    public static Object[] screen(
            ContainerScreenEventSnapshot snapshot,
            ContainerClientTickWindowSnapshot boundaryTick,
            String screenEventStage,
            String screenEventDisposition,
            String screenEventDropReason) {
        return screen(
                snapshot,
                boundaryTick,
                screenEventStage,
                screenEventDisposition,
                screenEventDropReason,
                "UNCORRELATED_SCREEN_ACTIVATION_DETAIL"
        );
    }

    public static Object[] screen(
            ContainerScreenEventSnapshot snapshot,
            ContainerClientTickWindowSnapshot boundaryTick,
            String screenEventStage,
            String screenEventDisposition,
            String screenEventDropReason,
            String diagnosticBudgetScope) {
        return merge(
                common(snapshot, boundaryTick),
                new Object[]{
                        "diagnosticBudgetScope", diagnosticBudgetScope,
                        "screenEventStage", screenEventStage,
                        "screenEventDisposition", screenEventDisposition,
                        "screenEventDropReason", screenEventDropReason,
                        "decisionReason", "NONE".equals(screenEventDropReason)
                                ? snapshot.diagnosticCorrelationReason()
                                : screenEventDropReason
                }
        );
    }

    public static Object[] downstream(
            ContainerScreenEventSnapshot snapshot,
            ContainerClientTickWindowSnapshot boundaryTick,
            String containerFlowBoundary,
            String decisionReason) {
        return merge(
                common(snapshot, boundaryTick),
                new Object[]{
                        "containerFlowBudgetScope", "FLOW_DETAIL",
                        "containerFlowBoundary", containerFlowBoundary,
                        "decisionReason", decisionReason == null ? "NONE" : decisionReason
                }
        );
    }

    public static Object[] eventBusTransport(
            ContainerScreenEventSnapshot snapshot,
            ContainerClientTickWindowSnapshot boundaryTick,
            String screenTransportBoundary,
            String screenTransportDisposition,
            String screenTransportReason,
            String diagnosticBudgetScope) {
        return merge(
                common(snapshot, boundaryTick),
                new Object[]{
                        "diagnosticBudgetScope", diagnosticBudgetScope,
                        "screenTransportBoundary", screenTransportBoundary,
                        "screenTransportDisposition", screenTransportDisposition,
                        "screenTransportReason", screenTransportReason,
                        "decisionReason", screenTransportReason
                }
        );
    }

    private static Object[] common(
            ContainerScreenEventSnapshot snapshot,
            ContainerClientTickWindowSnapshot boundaryTick) {
        ContainerClientTickWindowSnapshot tick = boundaryTick == null
                ? snapshot.tickWindow()
                : boundaryTick;
        Object[] interactionFields = snapshot.interaction() == null
                ? new Object[]{
                        "interactionId", "unavailable",
                        "interactionStartClientTick", -1L,
                        "interactResult", "unavailable",
                        "commandContextAvailable", false,
                        "commandRequestId", "unavailable",
                        "commandCorrelationId", "unavailable",
                        "commandSessionId", "unavailable"
                }
                : merge(
                        new Object[]{
                                "interactionId", snapshot.interaction().interactionId(),
                                "interactionStartClientTick", snapshot.interaction().interactionStartClientTick(),
                                "interactResult", snapshot.interaction().interactResult()
                        },
                        snapshot.interaction().commandContextFields()
                );
        return merge(new Object[]{
                "operationId", "unavailable",
                "openAttemptId", "unavailable",
                "correlationId", "unavailable",
                "routeOwnerClass", snapshot.interaction() == null
                        ? "unavailable"
                        : snapshot.interaction().routeOwnerClass(),
                "routeOwnerIdentity", snapshot.interaction() == null
                        ? "unavailable"
                        : snapshot.interaction().routeOwnerIdentity(),
                "rootAssignmentId", snapshot.interaction() == null
                        ? "unavailable"
                        : snapshot.interaction().rootAssignmentId(),
                "target", snapshot.target(),
                "targetBlockId", snapshot.targetBlockId(),
                "liveBlockId", snapshot.liveBlockId(),
                "targetFamily", snapshot.targetFamily(),
                "worldIdentity", snapshot.worldIdentity(),
                "dimension", snapshot.dimension(),
                "clientTickBoundarySerial", tick.activeClientTickSerialPresent()
                        ? tick.activeClientTickSerial()
                        : -1L,
                "screenOpenEventIdentity", snapshot.screenOpenEventIdentity(),
                "clientTickWindowState", tick.state(),
                "activeClientTickSerialPresent", tick.activeClientTickSerialPresent(),
                "activeClientTickSerial", tick.activeClientTickSerial(),
                "lastIssuedClientTickSerial", tick.lastIssuedClientTickSerial(),
                "lastPublishedClientTickBoundarySerial", tick.lastPublishedClientTickBoundarySerial(),
                "gateOutcome", "NOT_APPLICABLE",
                "coordinatorOutcome", "NOT_CALLED",
                "operationContextAvailable", false,
                "activeAttemptPresent", false,
                "eventPreOpen", snapshot.eventPreOpen(),
                "matchingBlockInteractEventObserved", false,
                "matchingBlockInteractEventCount", 0,
                "matchingBlockInteractEventConsumed", false,
                "diagnosticInteractionCandidateObserved", snapshot.interaction() != null,
                "diagnosticInteractionCandidateCount", snapshot.interaction() == null ? 0 : 1,
                "screenTailCandidate", false,
                "screenObjectIdentity", snapshot.screenObjectIdentity(),
                "screenName", snapshot.screenName(),
                "screenTypeExpected", snapshot.screenTypeExpected(),
                "screenTypeActual", snapshot.screenTypeActual(),
                "screenTypeMatched", snapshot.screenTypeMatched(),
                "screenIsHandled", snapshot.screenIsHandled(),
                "handledScreenHandlerIdentity", snapshot.handledScreenHandlerIdentity(),
                "playerHandlerIdentity", snapshot.playerHandlerIdentity(),
                "capturedSyncId", snapshot.capturedSyncId(),
                "liveSyncId", snapshot.liveSyncId(),
                "handlerTypeExpected", snapshot.handlerTypeExpected(),
                "handlerTypeActual", snapshot.handlerTypeActual(),
                "handlerTypeMatched", snapshot.handlerTypeMatched(),
                "eventScreenIsCurrentScreen", snapshot.eventScreenIsCurrentScreen(),
                "eventHandlerIsPlayerHandler", snapshot.eventHandlerIsPlayerHandler(),
                "diagnosticCorrelationCandidateAccepted", snapshot.diagnosticCorrelationCandidateAccepted(),
                "targetScreenAssociationProven", false,
                "diagnosticCorrelationReason", snapshot.diagnosticCorrelationReason()
        }, interactionFields);
    }

    public static Object[] aggregate(ContainerGuiDiagnosticAggregateSnapshot aggregate) {
        return new Object[]{
                "diagnosticDetailObservationCount", aggregate.diagnosticDetailObservationCount(),
                "diagnosticDetailDedupeSuppressedCount", aggregate.diagnosticDetailDedupeSuppressedCount(),
                "diagnosticDetailLocalAdmissionCount", aggregate.diagnosticDetailLocalAdmissionCount(),
                "diagnosticDetailLocalCapSuppressedCount", aggregate.diagnosticDetailLocalCapSuppressedCount(),
                "diagnosticDetailSharedAdmissionRejectedCount", aggregate.diagnosticDetailSharedAdmissionRejectedCount(),
                "diagnosticDetailPhysicalEmissionCount", aggregate.diagnosticDetailPhysicalEmissionCount(),
                "omittedCount", aggregate.omittedCount()
        };
    }

    public static Object[] merge(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second.clone();
        }
        if (second == null || second.length == 0) {
            return first.clone();
        }
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }
}
