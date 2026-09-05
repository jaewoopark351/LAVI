package lavi.minecraft.fabric.chatclef.bridge.command.connection.detach;

//20260905_kpopmodder: Emit only bounded command-ownership detach diagnostics.

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.control.FabricChatClefConnectionDetachResult;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefCommandContextUnbindDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.queue.FabricChatClefCommandQueueCompletion;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;

public final class FabricChatClefConnectionDetachLogEmitter {
    private final FabricChatClefBridgeDiagnostics diagnostics;

    public FabricChatClefConnectionDetachLogEmitter(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public void terminalSendDeferred(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachedEvent event
    ) {
        diagnostics.warn(
                "connection detach deferred while terminal result send is in flight request="
                        + context.requestId()
                        + " generation="
                        + event.connectionGeneration()
        );
    }

    public void activeDetach(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachedEvent event,
            FabricChatClefConnectionDetachDecision decision
    ) {
        diagnostics.warn(
                "connection detached with active command request="
                        + context.requestId()
                        + " generation="
                        + event.connectionGeneration()
                        + " reason="
                        + event.reason()
                        + " bound_root_match_reason="
                        + decision.rootMatchReason()
                        + " bound_root_ownership_for_detach="
                        + decision.boundRootOwnership()
                        + " detach_cancel_action="
                        + decision.cancelAction()
        );
    }

    public void cancellationSkipped(
            FabricChatClefCommandContext context,
            FabricChatClefConnectionDetachDecision decision
    ) {
        diagnostics.warn(
                "connection detach cancel skipped because current user task is not owned by request="
                        + context.requestId()
                        + " bound_root_match_reason="
                        + decision.rootMatchReason()
                        + " bound_root_ownership_for_detach="
                        + decision.boundRootOwnership()
                        + " detach_cancel_action="
                        + decision.cancelAction()
        );
    }

    public void cancellationUnavailable() {
        diagnostics.warn("connection detach cancel skipped because AltoClef user task chain is unavailable");
    }

    public void cancellingOwnedTask(String rootMatchReason) {
        diagnostics.warn("connection detach cancelling owned user task bound_root_match_reason=" + rootMatchReason);
    }

    public void detachedWithoutActive(
            FabricChatClefConnectionDetachResult detachResult,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            FabricChatClefTaskOwnershipSnapshot ownershipAfter
    ) {
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                "connection_detached_without_active_command",
                null,
                detachResult.activeBefore(),
                detachResult.activeAfter(),
                detachResult.changedQueueState(),
                ownershipBefore,
                ownershipAfter
        );
    }

    public void queueCompletion(
            FabricChatClefCommandQueueCompletion completion,
            FabricChatClefTaskOwnershipSnapshot ownershipBefore,
            FabricChatClefTaskOwnershipSnapshot ownershipAfter,
            FabricChatClefConnectionDetachDecision decision
    ) {
        FabricChatClefCommandContextUnbindDiagnostics.logBoundary(
                completion.reason(),
                completion.context(),
                completion.activeBefore(),
                completion.activeAfter(),
                completion.mutationApplied(),
                ownershipBefore,
                ownershipAfter,
                decision.boundRootOwnership(),
                decision.cancelAction()
        );
    }
}
