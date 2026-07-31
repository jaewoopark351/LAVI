package lavi.minecraft.diagnostics.postplace;

import net.minecraft.util.math.BlockPos;

//20260731_kpopmodder: Own post-place container intent lifecycle separately from log formatting.
final class PostPlaceContainerIntentTracker {
    private final Object lock = new Object();
    private PostPlaceContainerOpenIntent activeIntent;

    void begin(long operationId,
               String containerType,
               BlockPos targetPosition,
               String targetBlockState,
               long clientTickId) {
        if (operationId < 0 || targetPosition == null) {
            return;
        }
        synchronized (lock) {
            if (activeIntent != null
                    && activeIntent.operationId() == operationId
                    && !activeIntent.guiOpened()
                    && !activeIntent.timedOut()) {
                return;
            }
            activeIntent = new PostPlaceContainerOpenIntent(
                    operationId,
                    containerType,
                    targetPosition.toImmutable(),
                    targetBlockState,
                    clientTickId
            );
        }
    }

    PostPlaceContainerOpenIntent active(BlockPos targetPosition) {
        synchronized (lock) {
            if (activeIntent == null || targetPosition == null) {
                return null;
            }
            if (activeIntent.guiOpened() || activeIntent.timedOut()) {
                return null;
            }
            return activeIntent.targetPosition().equals(targetPosition) ? activeIntent : null;
        }
    }

    int attemptCount(long operationId) {
        synchronized (lock) {
            return matching(operationId) ? activeIntent.attemptCount() : 0;
        }
    }

    String lastInteractResult(long operationId) {
        synchronized (lock) {
            return matching(operationId) ? activeIntent.lastInteractResult() : "unavailable";
        }
    }

    long elapsedTicks(long operationId, long currentClientTickId) {
        synchronized (lock) {
            if (!matching(operationId)) {
                return -1;
            }
            return Math.max(0, currentClientTickId - activeIntent.startClientTickId());
        }
    }

    boolean markGuiOpened(long operationId) {
        synchronized (lock) {
            if (!matching(operationId) || activeIntent.guiOpened()) {
                return false;
            }
            activeIntent.markGuiOpened();
            return true;
        }
    }

    boolean markGuiTimeout(long operationId) {
        synchronized (lock) {
            if (!matching(operationId) || activeIntent.timedOut() || activeIntent.guiOpened()) {
                return false;
            }
            activeIntent.markTimedOut();
            return true;
        }
    }

    boolean markWarningLogged(long operationId) {
        synchronized (lock) {
            if (!matching(operationId) || activeIntent.warningLogged()) {
                return false;
            }
            activeIntent.markWarningLogged();
            return true;
        }
    }

    boolean isGuiOpened(long operationId) {
        synchronized (lock) {
            return matching(operationId) && activeIntent.guiOpened();
        }
    }

    void clear(long operationId) {
        synchronized (lock) {
            if (matching(operationId)) {
                activeIntent = null;
            }
        }
    }

    int recordInteraction(PostPlaceContainerOpenIntent intent, String phase, String result) {
        synchronized (lock) {
            if (intent == null || !matching(intent.operationId())) {
                return -1;
            }
            if ("HEAD".equals(phase)) {
                activeIntent.incrementAttemptCount();
            }
            if ("RETURN".equals(phase)) {
                activeIntent.setLastInteractResult(result);
            }
            return activeIntent.attemptCount();
        }
    }

    private boolean matching(long operationId) {
        return activeIntent != null && activeIntent.operationId() == operationId;
    }
}
