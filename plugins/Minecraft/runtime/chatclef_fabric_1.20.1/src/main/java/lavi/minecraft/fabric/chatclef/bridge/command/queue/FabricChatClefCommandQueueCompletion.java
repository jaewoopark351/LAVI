package lavi.minecraft.fabric.chatclef.bridge.command.queue;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

//20260814_kpopmodder: Added this value object so queue completion can be logged outside the queue monitor.
public final class FabricChatClefCommandQueueCompletion {
    private final String reason;
    private final FabricChatClefCommandContext context;
    private final FabricChatClefCommandContext activeBefore;
    private final FabricChatClefCommandContext activeAfter;
    private final boolean mutationApplied;

    private FabricChatClefCommandQueueCompletion(
            String reason,
            FabricChatClefCommandContext context,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            boolean mutationApplied
    ) {
        this.reason = nullToEmpty(reason);
        this.context = context;
        this.activeBefore = activeBefore;
        this.activeAfter = activeAfter;
        this.mutationApplied = mutationApplied;
    }

    public static FabricChatClefCommandQueueCompletion of(
            String reason,
            FabricChatClefCommandContext context,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            boolean mutationApplied
    ) {
        return new FabricChatClefCommandQueueCompletion(
                reason,
                context,
                activeBefore,
                activeAfter,
                mutationApplied
        );
    }

    public String reason() {
        return reason;
    }

    public FabricChatClefCommandContext context() {
        return context;
    }

    public FabricChatClefCommandContext activeBefore() {
        return activeBefore;
    }

    public FabricChatClefCommandContext activeAfter() {
        return activeAfter;
    }

    public boolean mutationApplied() {
        return mutationApplied;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
