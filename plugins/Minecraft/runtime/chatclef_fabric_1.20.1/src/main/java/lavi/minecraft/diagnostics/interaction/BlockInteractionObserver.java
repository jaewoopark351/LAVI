package lavi.minecraft.diagnostics.interaction;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

//20260805_kpopmodder: Expose block-interaction observations without changing Minecraft interaction behavior.
public interface BlockInteractionObserver {
    void beforeBlockInteraction(BlockInteractionContext context,
                                ClientPlayerEntity player,
                                Object hand,
                                BlockHitResult hitResult);

    void afterBlockInteraction(BlockInteractionContext context,
                               ClientPlayerEntity player,
                               Object hand,
                               BlockHitResult hitResult,
                               Object result);
}
