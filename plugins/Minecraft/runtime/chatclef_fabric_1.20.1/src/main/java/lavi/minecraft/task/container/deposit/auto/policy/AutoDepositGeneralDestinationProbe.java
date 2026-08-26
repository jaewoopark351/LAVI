package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.container.StoreInContainerTask;
import lavi.minecraft.task.container.deposit.DepositAllContainerEligibility;
import lavi.minecraft.task.container.deposit.DepositAllContainerSelector;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class AutoDepositGeneralDestinationProbe {
    private static final double ADOPTION_DISTANCE = 50.0;

    private final DepositAllContainerSelector selector = new DepositAllContainerSelector();

    public boolean hasUsableDestination(AltoClef mod) {
        if (mod.getPlayer() == null || mod.getWorld() == null) {
            return false;
        }
        DepositAllContainerEligibility eligibility = new DepositAllContainerEligibility();
        Optional<BlockPos> candidate = selector.select(
                mod,
                position -> eligibility.evaluate(
                        mod, position, StoreInContainerTask.CONTAINER_BLOCKS
                ).accepted(),
                StoreInContainerTask.CONTAINER_BLOCKS
        );
        return candidate.isPresent()
                && candidate.get().isWithinDistance(mod.getPlayer().getPos(), ADOPTION_DISTANCE);
    }
}
