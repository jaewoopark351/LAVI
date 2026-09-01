package lavi.minecraft.task.container.deposit.handoff.candidate;

import net.minecraft.block.Block;

import java.util.Objects;

//20260831_kpopmodder: Preserve the semantic container target for one controlled PLACE or OPEN route.
final class ContainerRouteSelection {
    private final Block candidate;
    private final boolean opensContainer;

    private ContainerRouteSelection(Block candidate, boolean opensContainer) {
        this.candidate = Objects.requireNonNull(candidate, "candidate");
        this.opensContainer = opensContainer;
    }

    static ContainerRouteSelection placement(Block candidate) {
        return new ContainerRouteSelection(candidate, false);
    }

    static ContainerRouteSelection open(Block candidate) {
        return new ContainerRouteSelection(candidate, true);
    }

    Block candidate() {
        return candidate;
    }

    boolean opensContainer() {
        return opensContainer;
    }
}
