package lavi.minecraft.diagnostics.interaction;

import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

//20260805_kpopmodder: Pair interaction HEAD and RETURN observations without mutating Minecraft state.
public final class BlockInteractionInvocationTracker {
    private final ThreadLocal<Deque<BlockInteractionContext>> activeInteractions =
            ThreadLocal.withInitial(ArrayDeque::new);

    public void begin(BlockInteractionContext context) {
        if (context != null) {
            activeInteractions.get().push(context);
        }
    }

    public BlockInteractionContext end(BlockPos targetPosition) {
        Deque<BlockInteractionContext> stack = activeInteractions.get();
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
}
