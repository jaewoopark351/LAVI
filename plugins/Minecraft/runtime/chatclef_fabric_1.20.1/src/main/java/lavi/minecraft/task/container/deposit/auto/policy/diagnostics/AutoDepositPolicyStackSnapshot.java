package lavi.minecraft.task.container.deposit.auto.policy.diagnostics;

import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositStackSnapshot;

//20260830_kpopmodder: Retain one already-captured physical stack and its final hard-protection verdict.
public record AutoDepositPolicyStackSnapshot(String stackFactId,
                                             String itemDecisionId,
                                             int slotIndex,
                                             String location,
                                             String itemId,
                                             int count,
                                             int maxCount,
                                             boolean selectedMainHand,
                                             boolean botProtected,
                                             boolean specialMetadata,
                                             String metadataFingerprint,
                                             String role,
                                             int equipmentScore,
                                             boolean food,
                                             boolean blockItem,
                                             boolean fallingBlock,
                                             boolean hardProtected) {
    public AutoDepositPolicyStackSnapshot {
        stackFactId = normalize(stackFactId);
        itemDecisionId = normalize(itemDecisionId);
        location = normalize(location);
        itemId = normalize(itemId);
        metadataFingerprint = normalize(metadataFingerprint);
        role = normalize(role);
    }

    public static AutoDepositPolicyStackSnapshot capture(String itemDecisionId,
                                                         int stackOrdinal,
                                                         AutoDepositStackSnapshot stack,
                                                         boolean hardProtected) {
        return new AutoDepositPolicyStackSnapshot(
                itemDecisionId + "-stack-" + stackOrdinal,
                itemDecisionId,
                stack.slotIndex(),
                stack.location().name(),
                stack.itemId(),
                stack.count(),
                stack.maxCount(),
                stack.selectedMainHand(),
                stack.botProtected(),
                stack.specialMetadata(),
                stack.metadataFingerprint(),
                stack.role().name(),
                stack.equipmentScore(),
                stack.food(),
                stack.blockItem(),
                stack.fallingBlock(),
                hardProtected
        );
    }

    public Object[] requiredFields(String inventorySnapshotId,
                                   String autoPlanId,
                                   long policyContextEpoch,
                                   StoreDepositAutomaticContext automaticContext) {
        StoreDepositAutomaticContext context = automaticContext == null
                ? StoreDepositAutomaticContext.unavailable()
                : automaticContext;
        return new Object[]{
                "inventorySnapshotId", inventorySnapshotId,
                "autoPlanId", autoPlanId,
                "autoOperationEpoch", context.available()
                        ? context.autoOperationEpoch()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "autoOperationId", context.available()
                        ? context.autoOperationId()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "maintenanceGenerationId", context.available()
                        ? context.maintenanceGenerationId()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "policyContextEpoch", policyContextEpoch,
                "itemDecisionId", itemDecisionId,
                "stackFactId", stackFactId,
                "physicalSourceSlot", slotIndex,
                "physicalStackLocation", location,
                "physicalStackItemId", itemId,
                "physicalStackCount", count,
                "physicalStackMaxCount", maxCount,
                "physicalStackSelectedMainHand", selectedMainHand,
                "physicalStackBotProtected", botProtected,
                "physicalStackSpecialMetadata", specialMetadata,
                "physicalStackMetadataFingerprint", metadataFingerprint,
                "physicalStackRole", role,
                "physicalStackEquipmentScore", equipmentScore,
                "physicalStackFood", food,
                "physicalStackBlockItem", blockItem,
                "physicalStackFallingBlock", fallingBlock,
                "physicalStackHardProtected", hardProtected,
                "observationComplete", true,
                "missingBoundaries", "NONE",
                "behavior_effect", "none"
        };
    }

    public String summary() {
        return stackFactId
                + "{slot=" + slotIndex
                + ",location=" + location
                + ",item=" + itemId
                + ",count=" + count
                + ",max=" + maxCount
                + ",selected=" + selectedMainHand
                + ",bot=" + botProtected
                + ",special=" + specialMetadata
                + ",metadata=" + metadataFingerprint
                + ",role=" + role
                + ",score=" + equipmentScore
                + ",food=" + food
                + ",block=" + blockItem
                + ",falling=" + fallingBlock
                + ",hardProtected=" + hardProtected
                + "}";
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
