package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.FabricChatClefTaskOwnershipCapturePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.FabricChatClefTaskOwnershipChainPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.FabricChatClefTaskOwnershipRootPayloadMap;

import java.util.HashMap;
import java.util.Map;

//20260808_kpopmodder: Isolate task ownership snapshot Map keys for diagnostics-only lifecycle evidence.
public final class FabricChatClefTaskOwnershipSnapshotPayloadMap {
    private FabricChatClefTaskOwnershipSnapshotPayloadMap() {
    }

    public static Map<String, Object> toMap(
            boolean available,
            String error,
            long capturedAtMs,
            long capturedClientTick,
            String captureThread,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag,
            boolean taskRunnerActive,
            String selectedChainClass,
            String selectedChainIdentity,
            boolean selectedChainIsUserTaskChain,
            String selectedChainTaskPath
    ) {
        Map<String, Object> payload = new HashMap<>();
        FabricChatClefTaskOwnershipCapturePayloadMap.writeTo(
                payload,
                available,
                error,
                capturedAtMs,
                capturedClientTick,
                captureThread
        );
        FabricChatClefTaskOwnershipRootPayloadMap.writeTo(
                payload,
                userTaskRoot,
                userTaskRootClass,
                userTaskRootIdentity,
                userTaskRootAssignmentId,
                userTaskRootGeneration,
                userTaskRunningIdle,
                nextTaskIdleFlag
        );
        FabricChatClefTaskOwnershipChainPayloadMap.writeTo(
                payload,
                taskRunnerActive,
                selectedChainClass,
                selectedChainIdentity,
                selectedChainIsUserTaskChain,
                selectedChainTaskPath
        );
        return payload;
    }
}
