package adris.altoclef.tasks.container.access;

import adris.altoclef.util.logging.StateChangeLogger;

import java.util.Objects;

//20260729_kpopmodder: Added this logger wrapper so container plan messages stay consistent after refactors.
public final class ContainerPlanLogger {

    private final StateChangeLogger debugLogger;
    private String lastPlanEventKey = "";

    public ContainerPlanLogger(StateChangeLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

    public void reset() {
        lastPlanEventKey = "";
    }

    public void logPlan(Object containerTarget,
                        ContainerTargetPlan plan,
                        boolean hasContainerItem,
                        String interactionContext) {
        logPlanEvent(plan.transitionKey(),
                "container plan transition: target=" + containerTarget
                        + ", action=" + plan.actionReason()
                        + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                        + ", usingPlacedContainer=" + plan.usingPlacedContainer()
                        + ", override=" + ContainerTaskDiagnostics.describePos(plan.overridePosition())
                        + ", placedTask=" + ContainerTaskDiagnostics.describePos(plan.placedTaskPosition())
                        + ", carriedPlaced=" + ContainerTaskDiagnostics.describePos(plan.carriedPlacedPosition())
                        + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                        + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                        + ", placeForceElapsed=" + plan.placeForceElapsed()
                        + ", justPlacedElapsed=" + plan.justPlacedElapsed()
                        + ", hasContainerItem=" + hasContainerItem
                        + ", " + interactionContext);

        debugLogger.state(plan.decisionStateKey(containerTarget),
                "container decision: target=" + containerTarget
                        + ", nearest=" + ContainerTaskDiagnostics.describeOptionalPos(plan.nearest())
                        + ", usingPlacedContainer=" + plan.usingPlacedContainer()
                        + ", override=" + ContainerTaskDiagnostics.describePos(plan.overridePosition())
                        + ", placedTask=" + ContainerTaskDiagnostics.describePos(plan.placedTaskPosition())
                        + ", carriedPlaced=" + ContainerTaskDiagnostics.describePos(plan.carriedPlacedPosition())
                        + ", walkCost=" + ContainerTaskDiagnostics.formatDouble(plan.walkCost())
                        + ", makeCost=" + ContainerTaskDiagnostics.formatDouble(plan.makeCost())
                        + ", placeForceElapsed=" + plan.placeForceElapsed()
                        + ", justPlacedElapsed=" + plan.justPlacedElapsed()
                        + ", hasContainerItem=" + hasContainerItem
                        + ", " + interactionContext);
    }

    private void logPlanEvent(String stateKey, String detail) {
        if (Objects.equals(lastPlanEventKey, stateKey)) {
            return;
        }
        lastPlanEventKey = stateKey;
        debugLogger.event(detail);
    }
}
