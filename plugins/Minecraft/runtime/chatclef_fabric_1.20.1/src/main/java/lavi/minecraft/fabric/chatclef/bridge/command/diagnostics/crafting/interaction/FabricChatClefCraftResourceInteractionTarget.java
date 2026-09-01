package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;

import java.util.Objects;

//20260901_kpopmodder: Keep an already-captured interaction target separate from projection policy.
record FabricChatClefCraftResourceInteractionTarget(
        CraftResourceStage resourceStage,
        CraftResourceTargetRole targetRole,
        String targetPosition,
        String sourceTargetBlockId,
        String sourceTargetKind) {

    FabricChatClefCraftResourceInteractionTarget {
        resourceStage = Objects.requireNonNull(resourceStage, "resourceStage");
        targetRole = Objects.requireNonNull(targetRole, "targetRole");
        targetPosition = Objects.requireNonNull(targetPosition, "targetPosition");
        sourceTargetBlockId = Objects.requireNonNull(
                sourceTargetBlockId,
                "sourceTargetBlockId"
        );
        sourceTargetKind = Objects.requireNonNull(sourceTargetKind, "sourceTargetKind");
    }
}
