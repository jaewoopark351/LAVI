package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.tasks.container.StoreInContainerTask;
import net.minecraft.block.Block;

import java.util.Arrays;

//20260827_kpopmodder: Keep trusted registration and execution on the existing supported storage boundary.
public final class AutoDepositTrustedContainerSupport {
    private AutoDepositTrustedContainerSupport() {
    }

    public static boolean isSupported(Block block) {
        return block != null && Arrays.stream(StoreInContainerTask.CONTAINER_BLOCKS)
                .anyMatch(block::equals);
    }
}
