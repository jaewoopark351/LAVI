package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.matching;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;
import lavi.minecraft.diagnostics.crafting.acquisition.target.position.CraftResourceTargetPosition;

import java.util.Locale;

//20260901_kpopmodder: Match stage, role, position, and block identity from existing observations.
public final class FabricChatClefCraftResourceInteractionTupleMatcher {
    public FabricChatClefCraftResourceInteractionTupleMatch evaluate(
            CraftResourceTargetTuple expected,
            CraftResourceStage observedStage,
            CraftResourceTargetRole observedRole,
            String observedPosition,
            String observedBlockId) {
        if (expected == null || observedStage == null || observedRole == null) {
            return gap("INTERACTION_TARGET_TUPLE_INPUT_UNAVAILABLE");
        }
        if (expected.resourceStage() == CraftResourceStage.UNKNOWN
                || observedStage == CraftResourceStage.UNKNOWN) {
            return gap("INTERACTION_RESOURCE_STAGE_UNKNOWN");
        }
        if (expected.resourceStage() != observedStage) {
            return gap("INTERACTION_RESOURCE_STAGE_DOES_NOT_MATCH_ACTIVE_TUPLE");
        }
        if (expected.targetRole() == CraftResourceTargetRole.UNKNOWN
                || observedRole == CraftResourceTargetRole.UNKNOWN) {
            return gap("INTERACTION_TARGET_ROLE_UNKNOWN");
        }
        if (expected.targetRole() != observedRole) {
            return gap("INTERACTION_TARGET_ROLE_DOES_NOT_MATCH_ACTIVE_TUPLE");
        }

        String expectedPosition = CraftResourceTargetPosition.canonicalize(
                expected.targetPosition()
        );
        String actualPosition = CraftResourceTargetPosition.canonicalize(observedPosition);
        if ("UNAVAILABLE".equals(expectedPosition)
                || "UNAVAILABLE".equals(actualPosition)) {
            return gap("INTERACTION_TARGET_POSITION_UNAVAILABLE");
        }
        if (!expectedPosition.equals(actualPosition)) {
            return gap("INTERACTION_TARGET_POSITION_DOES_NOT_MATCH_ACTIVE_TUPLE");
        }

        String actualBlockId = normalizeBlockId(observedBlockId);
        if (actualBlockId.isEmpty()) {
            return gap("INTERACTION_OBSERVED_BLOCK_ID_UNAVAILABLE");
        }
        if (expected.expectedBlockIds().isEmpty()) {
            return gap("INTERACTION_EXPECTED_BLOCK_IDS_UNAVAILABLE");
        }
        if (!expected.expectedBlockIds().contains(actualBlockId)) {
            return gap(expected.expectedBlockIdsOmittedCount() > 0
                    ? "INTERACTION_EXPECTED_BLOCK_ID_COVERAGE_INCOMPLETE"
                    : "INTERACTION_OBSERVED_BLOCK_NOT_IN_EXPECTED_IDS");
        }
        return new FabricChatClefCraftResourceInteractionTupleMatch(true, "NONE");
    }

    private static FabricChatClefCraftResourceInteractionTupleMatch gap(String reason) {
        return new FabricChatClefCraftResourceInteractionTupleMatch(false, reason);
    }

    private static String normalizeBlockId(String value) {
        if (value == null || value.isBlank() || "UNAVAILABLE".equalsIgnoreCase(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
