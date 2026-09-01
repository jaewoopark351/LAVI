package lavi.minecraft.diagnostics.interaction.ownership;

import adris.altoclef.tasksystem.Task;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.AtomicReference;

//20260901_kpopmodder: Consume one same-thread, exact-target owner token fail-closed.
public final class BlockInteractionOwnerTokenRegistry {
    private static final long MAX_TOKEN_AGE_TICKS = 1L;

    private final AtomicReference<Object> modeEpoch = new AtomicReference<>(new Object());
    private final ThreadLocal<EpochOwnerToken> ownerToken = new ThreadLocal<>();

    public void publish(Task sourceTask, BlockPos targetPosition, long captureClientTick) {
        EpochOwnerToken current = currentOwnerToken();
        if (sourceTask == null || targetPosition == null || captureClientTick < 0L) {
            current.token = null;
            return;
        }
        current.token = new BlockInteractionOwnerToken(
                sourceTask,
                targetPosition,
                captureClientTick
        );
    }

    public Task consume(BlockPos observedTargetPosition, long currentClientTick) {
        EpochOwnerToken current = currentOwnerToken();
        BlockInteractionOwnerToken token = current.token;
        current.token = null;
        if (token == null
                || observedTargetPosition == null
                || currentClientTick < token.captureClientTick()
                || currentClientTick - token.captureClientTick() > MAX_TOKEN_AGE_TICKS
                || !token.targetPosition().equals(observedTargetPosition)) {
            return null;
        }
        return token.sourceTask();
    }

    public void clearForModeTransition() {
        modeEpoch.set(new Object());
        ownerToken.remove();
    }

    private EpochOwnerToken currentOwnerToken() {
        Object currentEpoch = modeEpoch.get();
        EpochOwnerToken current = ownerToken.get();
        if (current == null || current.modeEpoch != currentEpoch) {
            current = new EpochOwnerToken(currentEpoch);
            ownerToken.set(current);
        }
        return current;
    }

    private static final class EpochOwnerToken {
        private final Object modeEpoch;
        private BlockInteractionOwnerToken token;

        private EpochOwnerToken(Object modeEpoch) {
            this.modeEpoch = modeEpoch;
        }
    }
}
