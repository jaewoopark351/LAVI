package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle;

import adris.altoclef.tasks.movement.IdleTask;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;

//20260822_kpopmodder: Keep root ownership classification command-agnostic and side-effect free.
public final class FabricChatClefRootOwnershipClassifier {
    public FabricChatClefRootOwnershipClassification classify(
            FabricChatClefTaskOwnershipEvidence before,
            FabricChatClefTaskOwnershipEvidence after
    ) {
        if (before == null || after == null || !before.available() || !after.available()) {
            return FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN;
        }
        if (!after.rootTaskPresent()) {
            return FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE;
        }
        if (isPreexistingUnchangedIdleRoot(before, after)) {
            return FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT;
        }
        if (isCommandOwnedRoot(before, after)) {
            return FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT;
        }
        return FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN;
    }

    private boolean isPreexistingUnchangedIdleRoot(
            FabricChatClefTaskOwnershipEvidence before,
            FabricChatClefTaskOwnershipEvidence after
    ) {
        return before.rootTaskPresent()
                && before.rootTask() == after.rootTask()
                && before.rootTask() instanceof IdleTask
                && after.rootTask() instanceof IdleTask
                && equalNonBlank(before.userTaskRootIdentity(), after.userTaskRootIdentity())
                && equalValidAssignment(before.userTaskRootAssignmentId(), after.userTaskRootAssignmentId())
                && before.userTaskRootGeneration() >= 0L
                && before.userTaskRootGeneration() == after.userTaskRootGeneration()
                && before.userTaskRunningIdle()
                && after.userTaskRunningIdle()
                && !before.nextTaskIdleFlag()
                && !after.nextTaskIdleFlag();
    }

    private boolean isCommandOwnedRoot(
            FabricChatClefTaskOwnershipEvidence before,
            FabricChatClefTaskOwnershipEvidence after
    ) {
        if (!validAssignment(after.userTaskRootAssignmentId()) || after.userTaskRootGeneration() < 0L) {
            return false;
        }
        if (!before.rootTaskPresent()) {
            return true;
        }
        boolean rootObjectChanged = before.rootTask() != after.rootTask();
        boolean assignmentChanged = !stringEquals(before.userTaskRootAssignmentId(), after.userTaskRootAssignmentId());
        boolean generationChanged = before.userTaskRootGeneration() != after.userTaskRootGeneration();
        return (rootObjectChanged || assignmentChanged || generationChanged)
                && (assignmentChanged || generationChanged);
    }

    private boolean equalNonBlank(String left, String right) {
        return !nullToEmpty(left).isBlank() && stringEquals(left, right);
    }

    private boolean equalValidAssignment(String left, String right) {
        return validAssignment(left) && stringEquals(left, right);
    }

    private boolean validAssignment(String assignment) {
        String value = nullToEmpty(assignment);
        return !value.isBlank() && !"none".equals(value) && !"unavailable".equals(value);
    }

    private boolean stringEquals(String left, String right) {
        return nullToEmpty(left).equals(nullToEmpty(right));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
