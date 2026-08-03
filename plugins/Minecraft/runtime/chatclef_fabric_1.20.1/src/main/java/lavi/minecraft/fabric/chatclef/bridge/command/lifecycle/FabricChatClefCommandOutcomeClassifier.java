package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;

//20260803_kpopmodder: Classify command completion from callback plus matching user task event.
public final class FabricChatClefCommandOutcomeClassifier {
    public FabricChatClefCommandTerminalDecision classify(FabricChatClefCommandExecution execution) {
        if (execution == null) {
            return FabricChatClefCommandTerminalDecision.waiting("no_active_execution");
        }
        FabricChatClefCommandTerminationObservation observation = execution.taskFinishedObservation();
        if (!execution.dispatchReturned()) {
            return FabricChatClefCommandTerminalDecision.waiting("dispatch_not_returned");
        }
        if (!execution.hasBoundRootTask()) {
            return classifyCommandWithoutUserTask(execution, observation);
        }
        if (observation == null) {
            return FabricChatClefCommandTerminalDecision.waiting("waiting_for_task_finished_event");
        }
        if (!execution.finishCallbackReceived()) {
            return FabricChatClefCommandTerminalDecision.waiting("waiting_for_command_callback");
        }
        if (!execution.matchesBoundRootTask(observation)) {
            return FabricChatClefCommandTerminalDecision.terminal(
                    "task_finished_event_identity_mismatch",
                    execution.unknownFromTaskIdentityMismatch(observation)
            );
        }
        if (!observation.stopStateAvailable()) {
            return FabricChatClefCommandTerminalDecision.terminal(
                    "task_finished_event_stop_state_unavailable",
                    execution.unknownFromTaskObservation(observation)
            );
        }
        if (observation.taskStopped()) {
            return FabricChatClefCommandTerminalDecision.terminal(
                    "matching_task_stopped",
                    execution.failedFromStoppedTask(observation)
            );
        }
        return FabricChatClefCommandTerminalDecision.terminal(
                "matching_task_finished",
                execution.completedFromTaskFinished(observation)
        );
    }

    private FabricChatClefCommandTerminalDecision classifyCommandWithoutUserTask(
            FabricChatClefCommandExecution execution,
            FabricChatClefCommandTerminationObservation observation
    ) {
        if (!execution.finishCallbackReceived()) {
            return FabricChatClefCommandTerminalDecision.waiting("waiting_for_callback_without_user_task");
        }
        if (observation != null && observation.taskPresent()) {
            return FabricChatClefCommandTerminalDecision.terminal(
                    "unexpected_task_finished_event_without_bound_task",
                    execution.unknownFromTaskIdentityMismatch(observation)
            );
        }
        return FabricChatClefCommandTerminalDecision.terminal(
                "callback_completed_without_user_task",
                execution.completedWithoutUserTask()
        );
    }
}
