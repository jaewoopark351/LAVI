package lavi.minecraft.diagnostics.postplace;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

//20260731_kpopmodder: Own post-place container intent tracking and observer dispatch outside the generic log facade.
public final class PostPlaceContainerDiagnosticState {
    private final PostPlaceContainerIntentTracker intents = new PostPlaceContainerIntentTracker();
    private final List<PostPlaceContainerInteractionObserver> observers = new CopyOnWriteArrayList<>();

    public void begin(long operationId,
                      String containerType,
                      BlockPos targetPosition,
                      String targetBlockState,
                      long clientTickId) {
        intents.begin(operationId, containerType, targetPosition, targetBlockState, clientTickId);
    }

    public PostPlaceContainerOpenIntent active(BlockPos targetPosition) {
        return intents.active(targetPosition);
    }

    public int attemptCount(long operationId) {
        return intents.attemptCount(operationId);
    }

    public String lastInteractResult(long operationId) {
        return intents.lastInteractResult(operationId);
    }

    public long elapsedTicks(long operationId, long currentClientTickId) {
        return intents.elapsedTicks(operationId, currentClientTickId);
    }

    public boolean markGuiOpened(long operationId) {
        return intents.markGuiOpened(operationId);
    }

    public boolean markGuiTimeout(long operationId) {
        return intents.markGuiTimeout(operationId);
    }

    public boolean markWarningLogged(long operationId) {
        return intents.markWarningLogged(operationId);
    }

    public boolean isGuiOpened(long operationId) {
        return intents.isGuiOpened(operationId);
    }

    public void clear(long operationId) {
        intents.clear(operationId);
    }

    public int recordInteraction(PostPlaceContainerOpenIntent intent, PostPlaceContainerInteractionPhase phase, String result) {
        return intents.recordInteraction(intent, phase, result);
    }

    public void registerObserver(PostPlaceContainerInteractionObserver observer) {
        if (observer == null || observers.contains(observer)) {
            return;
        }
        observers.add(observer);
    }

    public void notifyBeforeInteract(PostPlaceContainerOpenIntent intent) {
        for (PostPlaceContainerInteractionObserver observer : observers) {
            try {
                observer.beforeInteract(intent);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    public void notifyAfterInteract(PostPlaceContainerOpenIntent intent,
                                    ClientPlayerEntity player,
                                    Object result) {
        for (PostPlaceContainerInteractionObserver observer : observers) {
            try {
                observer.afterInteract(intent, player, result);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }
}
