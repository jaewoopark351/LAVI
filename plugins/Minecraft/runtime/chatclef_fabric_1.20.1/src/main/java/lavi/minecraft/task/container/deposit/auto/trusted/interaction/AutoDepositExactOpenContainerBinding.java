package lavi.minecraft.task.container.deposit.auto.trusted.interaction;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260827_kpopmodder: Share exact interaction-to-screen evidence without exposing command policy.
public interface AutoDepositExactOpenContainerBinding {
    AutoDepositExactOpenContainerBinding UNAVAILABLE = Optional::empty;

    Optional<BlockPos> currentExactPosition();

    default boolean matches(BlockPos position) {
        return position != null
                && currentExactPosition().filter(position::equals).isPresent();
    }
}
