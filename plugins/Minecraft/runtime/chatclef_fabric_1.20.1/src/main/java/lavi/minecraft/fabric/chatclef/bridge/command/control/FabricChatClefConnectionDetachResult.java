package lavi.minecraft.fabric.chatclef.bridge.command.control;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

import java.util.Optional;

//20260814_kpopmodder: Added this result to keep detach queue mutation separate from client-tick task cancellation.
public final class FabricChatClefConnectionDetachResult {
    private final FabricChatClefConnectionDetachedEvent event;
    private final FabricChatClefCommandContext activeBefore;
    private final FabricChatClefCommandContext activeAfter;
    private final FabricChatClefCommandContext detachedActive;
    private final int pendingRemovedCount;

    private FabricChatClefConnectionDetachResult(
            FabricChatClefConnectionDetachedEvent event,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            FabricChatClefCommandContext detachedActive,
            int pendingRemovedCount
    ) {
        this.event = event;
        this.activeBefore = activeBefore;
        this.activeAfter = activeAfter;
        this.detachedActive = detachedActive;
        this.pendingRemovedCount = pendingRemovedCount;
    }

    public static FabricChatClefConnectionDetachResult of(
            FabricChatClefConnectionDetachedEvent event,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            FabricChatClefCommandContext detachedActive,
            int pendingRemovedCount
    ) {
        return new FabricChatClefConnectionDetachResult(
                event,
                activeBefore,
                activeAfter,
                detachedActive,
                pendingRemovedCount
        );
    }

    public FabricChatClefConnectionDetachedEvent event() {
        return event;
    }

    public FabricChatClefCommandContext activeBefore() {
        return activeBefore;
    }

    public FabricChatClefCommandContext activeAfter() {
        return activeAfter;
    }

    public Optional<FabricChatClefCommandContext> detachedActive() {
        return Optional.ofNullable(detachedActive);
    }

    public int pendingRemovedCount() {
        return pendingRemovedCount;
    }

    public boolean changedQueueState() {
        return pendingRemovedCount > 0 || activeBefore != activeAfter || detachedActive != null;
    }
}
