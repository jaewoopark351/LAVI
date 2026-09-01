package lavi.minecraft.task.container.deposit.handoff.candidate;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import net.minecraft.block.Block;

//20260831_kpopmodder: Materialize controlled CHEST, BARREL, or freshly placed CHEST routes.
final class ContainerCandidateTaskFactory {
    private final Block chestCandidate;
    private final Block barrelCandidate;
    private final PlacementTaskSequence chestPlacements;
    private final PlacementTaskSequence barrelPlacements;
    private final OpenTaskProbe newlyPlacedChestOpen;

    ContainerCandidateTaskFactory(
            Block chestCandidate,
            Block barrelCandidate,
            PlacementTaskProbe chestPlacement,
            PlacementTaskProbe barrelPlacement,
            OpenTaskProbe newlyPlacedChestOpen) {
        this(
                chestCandidate,
                barrelCandidate,
                new PlacementTaskSequence(chestPlacement),
                new PlacementTaskSequence(barrelPlacement),
                newlyPlacedChestOpen
        );
    }

    ContainerCandidateTaskFactory(
            Block chestCandidate,
            Block barrelCandidate,
            PlacementTaskSequence chestPlacements,
            PlacementTaskSequence barrelPlacements,
            OpenTaskProbe newlyPlacedChestOpen) {
        this.chestCandidate = chestCandidate;
        this.barrelCandidate = barrelCandidate;
        this.chestPlacements = chestPlacements;
        this.barrelPlacements = barrelPlacements;
        this.newlyPlacedChestOpen = newlyPlacedChestOpen;
    }

    Task create(
            ContainerRouteSelection selection,
            DepositAllPlacementTaskOwner placementOwner) {
        Block candidate = selection.candidate();
        if (selection.opensContainer()) {
            if (candidate != newlyPlacedChestOpen.target()) {
                throw new AssertionError("Open route target does not match the controlled open child");
            }
            return newlyPlacedChestOpen;
        }
        if (candidate == chestCandidate) {
            return placementOwner.getOrCreate(chestCandidate, chestPlacements::next);
        }
        if (candidate == barrelCandidate) {
            return placementOwner.getOrCreate(barrelCandidate, barrelPlacements::next);
        }
        throw new AssertionError("Unexpected semantic container candidate");
    }

    int chestFactoryCalls() {
        return chestPlacements.creationCalls();
    }

    int barrelFactoryCalls() {
        return barrelPlacements.creationCalls();
    }
}
