//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.decision;

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminalDecision;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.state.RootTerminationState;
import lavi.minecraft.integration.lifecycle.root.UserRootCompletion;

//20260913_kpopmodder: Only a qualified owner retirement can end a missing-event wait; completion wins over Idle.
public final class RootRetirementClassifier {
    private RootRetirementClassifier() { }
    public static FabricChatClefCommandTerminalDecision classify(FabricChatClefCommandExecution execution) {
        RootTerminationState state = execution.rootTermination();
        if (!state.bound()) return null;
        if (!state.environmentValid()) return FabricChatClefCommandTerminalDecision.waiting("user_root_environment_unavailable");
        if (execution.taskFinishedObservation() != null && execution.finishCallbackReceived()) return null;
        if (state.pending()) return FabricChatClefCommandTerminalDecision.waiting("pending_root_completion_prefix");
        if (!state.ready()) return null;
        if (execution.userStopBound()) return FabricChatClefCommandTerminalDecision.terminal(
                "user_stop_requested", execution.cancelledFromUserStop());
        UserRootCompletion completion = state.completion();
        if (completion != null) {
            if (completion.stopStateAvailable() && completion.stopped()) return FabricChatClefCommandTerminalDecision.terminal(
                    "matching_root_stopped_without_event", execution.failedFromRootRetirement("matching_root_stopped_without_event"));
            // The chain completion boundary is authoritative, but it is not a fabricated callback/event or goal proof.
            String reason = completion.stopStateAvailable() ? "root_completed_without_complete_event_handoff" : "root_completion_state_unavailable";
            return FabricChatClefCommandTerminalDecision.terminal(reason, execution.unknownFromRootCompletion(reason));
        }
        if (state.reason().isEmpty()) return null;
        return FabricChatClefCommandTerminalDecision.terminal(state.reason(), execution.failedFromRootRetirement(state.reason()));
    }
}
//#endif
