package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

import java.util.Optional;

//20260901_kpopmodder: Observe exact queue/context unbind evidence after its authority log.
public final class FabricChatClefCraftResourceTerminalContextUnbindObserver {
    private static final String SOURCE_EVENT = "COMMAND_CONTEXT_UNBIND_BOUNDARY";
    private static final FabricChatClefCraftResourceTerminalSendProjector SENDS =
            new FabricChatClefCraftResourceTerminalSendProjector();

    private FabricChatClefCraftResourceTerminalContextUnbindObserver() {
    }

    public static void observe(
            String unbindReason,
            FabricChatClefCommandContext context,
            boolean mutationApplied,
            String detachCancelAction
    ) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        Optional<FabricChatClefCraftResourceTerminalScopeBinding> exact =
                FabricChatClefCraftResourceTerminalDiagnostics.exact(context);
        if (exact.isEmpty()) {
            return;
        }
        FabricChatClefCraftResourceTerminalScopeBinding binding = exact.get();
        long clientTick = ChatClefDiagnostics.currentClientTickId();
        long monotonicNanos = System.nanoTime();
        FabricChatClefCraftResourceTerminalDiagnostics.recordSend(
                binding,
                SENDS.fromContext(context),
                SOURCE_EVENT,
                clientTick,
                monotonicNanos
        );
        if ("cancel_owned_root".equals(detachCancelAction)) {
            FabricChatClefCraftResourceTerminalDiagnostics.recordOwnedRootCancellation(
                    binding,
                    "OWNED_ROOT_CANCEL_REQUESTED_TASK_FINISH_UNAVAILABLE",
                    SOURCE_EVENT,
                    clientTick,
                    monotonicNanos
            );
        }
        FabricChatClefCraftResourceTerminalDiagnostics.recordQueueContextCleared(
                binding,
                mutationApplied,
                unbindReason,
                SOURCE_EVENT,
                clientTick,
                monotonicNanos
        );
    }
}
