package lavi.minecraft.diagnostics.interaction;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//20260805_kpopmodder: Keep diagnostic observer fan-out separate from interaction tracking.
public final class BlockInteractionObserverRegistry {
    private final List<BlockInteractionObserver> observers = new CopyOnWriteArrayList<>();

    public void register(BlockInteractionObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void notifyBefore(BlockInteractionContext context,
                             ClientPlayerEntity player,
                             Object hand,
                             BlockHitResult hitResult) {
        for (BlockInteractionObserver observer : observers) {
            try {
                observer.beforeBlockInteraction(context, player, hand, hitResult);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    public void notifyAfter(BlockInteractionContext context,
                            ClientPlayerEntity player,
                            Object hand,
                            BlockHitResult hitResult,
                            Object result) {
        for (BlockInteractionObserver observer : observers) {
            try {
                observer.afterBlockInteraction(context, player, hand, hitResult, result);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }
}
