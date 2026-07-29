package adris.altoclef.tasks.container.access;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260729_kpopmodder: Added this value object to make existing-vs-new container decisions explicit.
public final class ContainerTargetPlan {

    private final Optional<BlockPos> nearest;
    private final boolean usingPlacedContainer;
    private final BlockPos overridePosition;
    private final BlockPos placedTaskPosition;
    private final BlockPos carriedPlacedPosition;
    private final double walkCost;
    private final double makeCost;
    private final boolean placeForceElapsed;
    private final boolean justPlacedElapsed;
    private final String actionReason;

    public ContainerTargetPlan(Optional<BlockPos> nearest,
                               boolean usingPlacedContainer,
                               BlockPos overridePosition,
                               BlockPos placedTaskPosition,
                               BlockPos carriedPlacedPosition,
                               double walkCost,
                               double makeCost,
                               boolean placeForceElapsed,
                               boolean justPlacedElapsed) {
        this.nearest = nearest;
        this.usingPlacedContainer = usingPlacedContainer;
        this.overridePosition = overridePosition;
        this.placedTaskPosition = placedTaskPosition;
        this.carriedPlacedPosition = carriedPlacedPosition;
        this.walkCost = walkCost;
        this.makeCost = makeCost;
        this.placeForceElapsed = placeForceElapsed;
        this.justPlacedElapsed = justPlacedElapsed;
        this.actionReason = describeActionReason();
    }

    public Optional<BlockPos> nearest() {
        return nearest;
    }

    public BlockPos requireTargetPosition() {
        return nearest.orElseThrow();
    }

    public boolean usingPlacedContainer() {
        return usingPlacedContainer;
    }

    public BlockPos overridePosition() {
        return overridePosition;
    }

    public BlockPos placedTaskPosition() {
        return placedTaskPosition;
    }

    public BlockPos carriedPlacedPosition() {
        return carriedPlacedPosition;
    }

    public double walkCost() {
        return walkCost;
    }

    public double makeCost() {
        return makeCost;
    }

    public boolean placeForceElapsed() {
        return placeForceElapsed;
    }

    public boolean justPlacedElapsed() {
        return justPlacedElapsed;
    }

    public String actionReason() {
        return actionReason;
    }

    public boolean shouldResetPlaceForceTimer() {
        return !usingPlacedContainer && walkCost > makeCost;
    }

    public boolean shouldUseNewContainer(boolean currentPlaceForceElapsed, boolean currentJustPlacedElapsed) {
        return nearest.isEmpty()
                || (!usingPlacedContainer && !currentPlaceForceElapsed && currentJustPlacedElapsed);
    }

    public String transitionKey() {
        return actionReason + ":" + ContainerTaskDiagnostics.describeOptionalPos(nearest);
    }

    public String decisionStateKey(Object containerTarget) {
        return "container decision:" + containerTarget
                + ":nearest=" + ContainerTaskDiagnostics.describeOptionalPos(nearest)
                + ":placed=" + usingPlacedContainer
                + ":walk=" + ContainerTaskDiagnostics.formatDouble(walkCost);
    }

    private String describeActionReason() {
        if (nearest.isEmpty()) {
            return "no-reachable-container";
        }
        if (!usingPlacedContainer && walkCost > makeCost) {
            return "new-container-cheaper";
        }
        if (!usingPlacedContainer && !placeForceElapsed && justPlacedElapsed) {
            return "place-force-window-active";
        }
        return "use-existing-container";
    }
}
