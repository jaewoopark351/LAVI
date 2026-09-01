package lavi.minecraft.diagnostics.interaction;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

//20260805_kpopmodder: Pair interaction HEAD and RETURN observations without mutating Minecraft state.
public final class BlockInteractionInvocationTracker {
    private final AtomicReference<Object> modeEpoch = new AtomicReference<>(new Object());
    private final ThreadLocal<EpochInteractions> activeInteractions = new ThreadLocal<>();

    public void begin(BlockInteractionContext context) {
        if (context != null) {
            currentInteractions().push(context);
        }
    }

    public BlockInteractionContext end(BlockPos targetPosition) {
        Deque<BlockInteractionContext> stack = currentInteractions();
        if (stack.isEmpty()) {
            return null;
        }
        BlockInteractionContext top = stack.peek();
        if (top.targetMatches(targetPosition)) {
            return stack.pop();
        }
        Iterator<BlockInteractionContext> iterator = stack.iterator();
        while (iterator.hasNext()) {
            BlockInteractionContext context = iterator.next();
            if (context.targetMatches(targetPosition)) {
                iterator.remove();
                return context;
            }
        }
        return null;
    }

    public void clearForModeTransition() {
        modeEpoch.set(new Object());
        activeInteractions.remove();
    }

    private Deque<BlockInteractionContext> currentInteractions() {
        Object currentEpoch = modeEpoch.get();
        EpochInteractions current = activeInteractions.get();
        if (current == null || current.modeEpoch != currentEpoch) {
            current = new EpochInteractions(currentEpoch, new ArrayDeque<>());
            activeInteractions.set(current);
        }
        return current.interactions;
    }

    private record EpochInteractions(Object modeEpoch,
                                     Deque<BlockInteractionContext> interactions) {
    }
}
