package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.List;
import java.util.StringJoiner;

//20260808_kpopmodder: Read UserTaskChain ownership state separately from command lifecycle decisions.
public final class FabricChatClefTaskOwnershipSnapshotReader {
    public FabricChatClefTaskOwnershipSnapshot ownershipSnapshot() {
        return ownershipEvidence().ownershipSnapshot();
    }

    public FabricChatClefTaskOwnershipEvidence ownershipEvidence() {
        try {
            long capturedAtMs = System.currentTimeMillis();
            long capturedAtNanos = System.nanoTime();
            long capturedClientTick = ChatClefDiagnostics.currentClientTickId();
            String captureThread = Thread.currentThread().getName();
            AltoClef mod = AltoClef.getInstance();
            UserTaskChain userTaskChain = mod == null ? null : mod.getUserTaskChain();
            TaskRunner taskRunner = mod == null ? null : mod.getTaskRunner();
            Task userRoot = userTaskChain == null ? null : userTaskChain.getCurrentTask();
            TaskChain selectedChain = taskRunner == null ? null : taskRunner.getCurrentTaskChain();
            FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(userRoot);
            FabricChatClefTaskOwnershipSnapshot ownershipSnapshot = FabricChatClefTaskOwnershipSnapshot.of(
                    capturedAtMs,
                    capturedClientTick,
                    captureThread,
                    rootSnapshot,
                    ChatClefDiagnostics.className(userRoot),
                    taskIdentity(userRoot),
                    userTaskChain == null ? "unavailable" : userTaskChain.diagnosticRootAssignmentId(),
                    userTaskChain == null ? -1L : userTaskChain.diagnosticRootGeneration(),
                    userTaskChain != null && userTaskChain.diagnosticRunningIdleTaskFlag(),
                    userTaskChain != null && userTaskChain.diagnosticNextTaskIdleFlag(),
                    taskRunner != null && taskRunner.isActive(),
                    selectedChain == null ? "" : selectedChain.getClass().getName(),
                    selectedChain == null ? "none" : Integer.toHexString(System.identityHashCode(selectedChain)),
                    selectedChain instanceof UserTaskChain,
                    selectedChainTaskPath(selectedChain)
            );
            return FabricChatClefTaskOwnershipEvidence.of(
                    userRoot,
                    rootSnapshot,
                    ownershipSnapshot,
                    capturedAtMs,
                    capturedAtNanos,
                    capturedClientTick,
                    captureThread
            );
        } catch (Throwable error) {
            return FabricChatClefTaskOwnershipEvidence.unavailable(error);
        }
    }

    private static String selectedChainTaskPath(TaskChain selectedChain) {
        if (selectedChain == null) {
            return "";
        }
        try {
            List<Task> tasks = selectedChain.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                return "";
            }
            StringJoiner joiner = new StringJoiner(" > ");
            int count = 0;
            for (Task task : tasks) {
                joiner.add(ChatClefDiagnostics.className(task) + "#" + taskIdentity(task));
                count++;
                if (count >= 8) {
                    joiner.add("...");
                    break;
                }
            }
            return joiner.toString();
        } catch (Throwable error) {
            return "unavailable#error=" + error.getClass().getSimpleName();
        }
    }

    private static String taskIdentity(Task task) {
        return task == null ? "none" : Integer.toHexString(System.identityHashCode(task));
    }
}
