package lavi.minecraft.task.container.deposit.handoff.candidate;

import net.minecraft.block.Block;

//20260831_kpopmodder: Provide one controlled PLACE-or-OPEN route decision per parent tick.
final class ContainerRouteProbe {
    private ContainerRouteSelection selection;
    private int evaluationCalls;

    void selectPlacement(Block candidate) {
        selection = ContainerRouteSelection.placement(candidate);
    }

    void selectOpen(Block candidate) {
        selection = ContainerRouteSelection.open(candidate);
    }

    ContainerRouteSelection evaluate() {
        if (selection == null) {
            throw new AssertionError("A controlled route must be selected before evaluation");
        }
        evaluationCalls++;
        return selection;
    }

    int evaluationCalls() {
        return evaluationCalls;
    }
}
