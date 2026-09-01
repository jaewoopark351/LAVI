package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.FabricChatClefCraftResourceTerminalContextUnbindObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;

//20260808_kpopmodder: Log command context unbind mutations without changing queue ownership.
public final class FabricChatClefCommandContextUnbindDiagnostics {
    private static final FabricChatClefTaskOwnershipSnapshotReader OWNERSHIP_READER =
            new FabricChatClefTaskOwnershipSnapshotReader();

    private FabricChatClefCommandContextUnbindDiagnostics() {
    }

    public static FabricChatClefTaskOwnershipSnapshot captureOwnershipSnapshot() {
        return OWNERSHIP_READER.ownershipSnapshot();
    }

    public static void logBoundary(
            String unbindReason,
            FabricChatClefCommandContext context,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            boolean mutationApplied,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            FabricChatClefTaskOwnershipSnapshot ownershipAfter
    ) {
        logBoundary(
                unbindReason,
                context,
                activeBefore,
                activeAfter,
                mutationApplied,
                ownershipBefore,
                ownershipAfter,
                "",
                ""
        );
    }

    public static void logBoundary(
            String unbindReason,
            FabricChatClefCommandContext context,
            FabricChatClefCommandContext activeBefore,
            FabricChatClefCommandContext activeAfter,
            boolean mutationApplied,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            FabricChatClefTaskOwnershipSnapshot ownershipAfter,
            String boundRootOwnershipForDetach,
            String detachCancelAction
    ) {
        ChatClefDiagnostics.logLifecycleBoundary(
                "COMMAND_CONTEXT_UNBIND_BOUNDARY",
                "command_context_unbind_boundary",
                null,
                "context_unbind_reason", unbindReason,
                "request_id", requestId(context),
                "correlation_id", correlationId(context),
                "connection_generation", connectionGeneration(context),
                "queue_active_request_before", requestId(activeBefore),
                "queue_active_request_after", requestId(activeAfter),
                "queue_complete_result", mutationApplied,
                "user_root_before_unbind_class", ownershipBefore.userTaskRootClass(),
                "user_root_before_unbind_identity", ownershipBefore.userTaskRootIdentity(),
                "user_root_before_unbind_assignment_id", ownershipBefore.userTaskRootAssignmentId(),
                "user_root_before_unbind_generation", ownershipBefore.userTaskRootGeneration(),
                "user_root_before_unbind_running_idle", ownershipBefore.userTaskRunningIdle(),
                "user_root_after_unbind_class", ownershipAfter.userTaskRootClass(),
                "user_root_after_unbind_identity", ownershipAfter.userTaskRootIdentity(),
                "user_root_after_unbind_assignment_id", ownershipAfter.userTaskRootAssignmentId(),
                "user_root_after_unbind_generation", ownershipAfter.userTaskRootGeneration(),
                "user_root_after_unbind_running_idle", ownershipAfter.userTaskRunningIdle(),
                "ownership_before_unbind", ownershipBefore.toMap(),
                "ownership_after_unbind", ownershipAfter.toMap(),
                "bound_root_ownership_for_detach", nullToEmpty(boundRootOwnershipForDetach),
                "detach_cancel_action", nullToEmpty(detachCancelAction),
                "unbind_at_ms", System.currentTimeMillis(),
                "unbind_client_tick", ChatClefDiagnostics.currentClientTickId(),
                "behavior_effect", "none"
        );
        try {
            FabricChatClefCraftResourceTerminalContextUnbindObserver.observe(
                    unbindReason,
                    context,
                    mutationApplied,
                    detachCancelAction
            );
        } catch (RuntimeException | LinkageError ignored) {
            // A diagnostics-only projection failure must not alter context unbind.
        }
    }

    private static String requestId(FabricChatClefCommandContext context) {
        return context == null ? "" : context.requestId();
    }

    private static String correlationId(FabricChatClefCommandContext context) {
        return context == null ? "" : context.correlationId();
    }

    private static long connectionGeneration(FabricChatClefCommandContext context) {
        return context == null ? -1L : context.connectionGeneration();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
