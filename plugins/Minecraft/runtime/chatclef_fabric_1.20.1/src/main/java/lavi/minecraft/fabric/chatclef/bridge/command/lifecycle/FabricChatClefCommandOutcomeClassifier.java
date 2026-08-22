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
        FabricChatClefRootOwnershipClassification classification = execution.rootOwnershipClassification();
        if (classification == null) {
            return FabricChatClefCommandTerminalDecision.waiting("root_ownership_unknown");
        }
        switch (classification) {
            case PREEXISTING_UNCHANGED_IDLE_ROOT:
                return classifyPreexistingUnchangedIdleRoot(execution);
            case OWNERSHIP_UNKNOWN:
                return FabricChatClefCommandTerminalDecision.waiting("root_ownership_unknown");
            case NO_ROOT_VISIBLE:
                return classifyCommandWithoutUserTask(execution, observation);
            case COMMAND_OWNED_ROOT:
                if (!execution.hasBoundRootTask()) {
                    return FabricChatClefCommandTerminalDecision.waiting("command_owned_root_missing_bound_task");
                }
                break;
            default:
                return FabricChatClefCommandTerminalDecision.waiting("root_ownership_unclassified");
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

    private FabricChatClefCommandTerminalDecision classifyPreexistingUnchangedIdleRoot(
            FabricChatClefCommandExecution execution
    ) {
        if (!execution.finishCallbackReceived()) {
            return FabricChatClefCommandTerminalDecision.waiting("waiting_for_command_callback");
        }
        if (!execution.finishCallbackFirstObservedBeforeDispatchReturn()) {
            return FabricChatClefCommandTerminalDecision.waiting("finish_callback_not_synchronous");
        }
        if (execution.taskFinishedObservation() != null) {
            return FabricChatClefCommandTerminalDecision.waiting("task_finished_event_attached_before_idle_root_classification");
        }
        if (!execution.preexistingIdleRootStabilityQualified()) {
            return FabricChatClefCommandTerminalDecision.waiting("waiting_for_preexisting_idle_root_stability");
        }
        return FabricChatClefCommandTerminalDecision.terminal(
                "preexisting_unchanged_idle_root_without_command_owned_root",
                execution.unknownFromPreexistingUnchangedIdleRoot()
        );
    }
}
