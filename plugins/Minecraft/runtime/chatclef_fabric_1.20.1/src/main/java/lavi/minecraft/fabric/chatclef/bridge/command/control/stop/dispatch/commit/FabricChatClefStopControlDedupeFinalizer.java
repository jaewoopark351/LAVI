package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.dispatch.commit;

//20260905_kpopmodder: Finalize one STOP identity in the bounded dedupe registry.

import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership.FabricChatClefStopControlDedupeRegistry;

public final class FabricChatClefStopControlDedupeFinalizer {
    private final FabricChatClefStopControlDedupeRegistry dedupeRegistry;

    public FabricChatClefStopControlDedupeFinalizer(
            FabricChatClefStopControlDedupeRegistry dedupeRegistry
    ) {
        this.dedupeRegistry = dedupeRegistry;
    }

    public void finalizeIdentity(FabricChatClefStopControlContext context) {
        dedupeRegistry.markTerminal(context.request().identity());
    }
}
