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
        notifyBefore(context, player, hand, hitResult, true);
    }

    public void notifyBefore(BlockInteractionContext context,
                             ClientPlayerEntity player,
                             Object hand,
                             BlockHitResult hitResult,
                             boolean sourceEmissionCompleted) {
        for (BlockInteractionObserver observer : observers) {
            if (observer.requiresCompletedSourceEmission() && !sourceEmissionCompleted) {
                continue;
            }
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
        notifyAfter(context, player, hand, hitResult, result, true);
    }

    public void notifyAfter(BlockInteractionContext context,
                            ClientPlayerEntity player,
                            Object hand,
                            BlockHitResult hitResult,
                            Object result,
                            boolean sourceEmissionCompleted) {
        for (BlockInteractionObserver observer : observers) {
            if (observer.requiresCompletedSourceEmission() && !sourceEmissionCompleted) {
                continue;
            }
            try {
                observer.afterBlockInteraction(context, player, hand, hitResult, result);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }
}
