package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipNextTaskIdlePayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRootAssignmentPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRootClassPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRootGenerationPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRootIdentityPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRootPayloadMap;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.ownership.root.FabricChatClefTaskOwnershipUserTaskRunningIdlePayloadMap;

import java.util.Map;

//20260808_kpopmodder: Keep UserTask root ownership fields separate from selected-chain serialization.
public final class FabricChatClefTaskOwnershipRootPayloadMap {
    private FabricChatClefTaskOwnershipRootPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskSnapshot userTaskRoot,
            String userTaskRootClass,
            String userTaskRootIdentity,
            String userTaskRootAssignmentId,
            long userTaskRootGeneration,
            boolean userTaskRunningIdle,
            boolean nextTaskIdleFlag
    ) {
        FabricChatClefTaskOwnershipUserTaskRootPayloadMap.writeTo(payload, userTaskRoot);
        FabricChatClefTaskOwnershipUserTaskRootClassPayloadMap.writeTo(payload, userTaskRootClass);
        FabricChatClefTaskOwnershipUserTaskRootIdentityPayloadMap.writeTo(payload, userTaskRootIdentity);
        FabricChatClefTaskOwnershipUserTaskRootAssignmentPayloadMap.writeTo(payload, userTaskRootAssignmentId);
        FabricChatClefTaskOwnershipUserTaskRootGenerationPayloadMap.writeTo(payload, userTaskRootGeneration);
        FabricChatClefTaskOwnershipUserTaskRunningIdlePayloadMap.writeTo(payload, userTaskRunningIdle);
        FabricChatClefTaskOwnershipNextTaskIdlePayloadMap.writeTo(payload, nextTaskIdleFlag);
    }
}
