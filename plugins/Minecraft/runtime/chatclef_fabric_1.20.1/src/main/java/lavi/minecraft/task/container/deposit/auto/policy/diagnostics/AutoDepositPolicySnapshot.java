package lavi.minecraft.task.container.deposit.auto.policy.diagnostics;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

//20260830_kpopmodder: Preserve one immutable policy decision boundary without rescanning inventory state.
public record AutoDepositPolicySnapshot(String inventorySnapshotId,
                                        String autoPlanId,
                                        long policyContextEpoch,
                                        StoreDepositAutomaticContext automaticContext,
                                        String planningStatus,
                                        String planningReason,
                                        int occupiedSlots,
                                        int totalSlots,
                                        int targetReliefSlots,
                                        int expectedFreedSlots,
                                        String selectedGeneralTargets,
                                        String selectedTrustedTargets,
                                        String selectedAggregateTargets,
                                        String destinationDecision,
                                        String trustedDestination,
                                        int trustedCandidateCount,
                                        List<AutoDepositPolicyItemSnapshot> itemDecisions,
                                        boolean observationComplete,
                                        String missingBoundaries) {
    public AutoDepositPolicySnapshot {
        inventorySnapshotId = normalize(inventorySnapshotId);
        autoPlanId = normalize(autoPlanId);
        automaticContext = automaticContext == null
                ? StoreDepositAutomaticContext.unavailable()
                : automaticContext;
        planningStatus = normalize(planningStatus);
        planningReason = normalize(planningReason);
        selectedGeneralTargets = normalize(selectedGeneralTargets);
        selectedTrustedTargets = normalize(selectedTrustedTargets);
        selectedAggregateTargets = normalize(selectedAggregateTargets);
        destinationDecision = normalize(destinationDecision);
        trustedDestination = normalize(trustedDestination);
        itemDecisions = itemDecisions == null ? List.of() : List.copyOf(itemDecisions);
        missingBoundaries = normalize(missingBoundaries);
    }

    public static AutoDepositPolicySnapshot capture(AutoDepositPlan plan,
                                                    DepositAllInventoryPressureSnapshot pressure,
                                                    String status,
                                                    String reason,
                                                    StoreDepositAutomaticContext automaticContext) {
        int occupied = pressure == null ? -1 : pressure.occupiedSlots();
        int total = pressure == null ? -1 : pressure.totalSlots();
        if (plan == null) {
            return new AutoDepositPolicySnapshot(
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    -1L,
                    automaticContext,
                    status,
                    reason,
                    occupied,
                    total,
                    pressure == null ? -1 : pressure.requiredReliefSlots(),
                    -1,
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    "UNAVAILABLE",
                    0,
                    List.of(),
                    false,
                    "POLICY_PLAN_NOT_RETAINED"
            );
        }
        long epoch = plan.context().epoch();
        boolean hasGeneral = plan.generalTargets().length > 0;
        boolean hasTrusted = plan.trustedTargets().length > 0;
        String destinationDecision = hasGeneral && hasTrusted
                ? "GENERAL_AND_TRUSTED_ONLY"
                : hasTrusted ? "TRUSTED_ONLY" : hasGeneral ? "GENERAL_CONTAINER" : "NO_SELECTED_DESTINATION";
        List<AutoDepositPolicyItemSnapshot> itemDecisions = plan.policyItemDecisions();
        String generalTargets = ChatClefDiagnostics.itemTargets(plan.generalTargets());
        String trustedTargets = ChatClefDiagnostics.itemTargets(plan.trustedTargets());
        String aggregateTargets = ChatClefDiagnostics.itemTargets(plan.allTargets());
        String itemDecisionFingerprint = itemDecisions.stream()
                .map(AutoDepositPolicyItemSnapshot::summary)
                .reduce("NONE", (left, right) -> left.equals("NONE") ? right : left + "|" + right);
        boolean diagnosticPolicyObservationRetained = !"UNAVAILABLE_DIAGNOSTICS_OFF".equals(
                plan.diagnosticInventoryFingerprint()
        );
        String inventorySnapshotId = diagnosticPolicyObservationRetained
                ? diagnosticId(
                        "inventory",
                        epoch + "|" + occupied + "/" + total + "|" + plan.diagnosticInventoryFingerprint()
                )
                : "UNAVAILABLE_DIAGNOSTICS_OFF";
        String trustedCandidateFingerprint = plan.trustedCandidates().stream()
                .map(candidate -> candidate.destinationId()
                        + ":" + candidate.position()
                        + ":empty=" + candidate.cachedEmptySlots()
                        + ":distanceSquared=" + candidate.distanceSquared()
                        + ":state=" + candidate.observedState())
                .reduce("NONE", (left, right) -> left.equals("NONE") ? right : left + "|" + right);
        String autoPlanId = diagnosticId(
                "plan",
                inventorySnapshotId + "|" + generalTargets + "|" + trustedTargets
                        + "|" + plan.targetReliefSlots() + "|" + plan.expectedFreedSlots()
                        + "|" + itemDecisionFingerprint
                        + "|trustedRevision=" + plan.trustedRevision()
                        + "|trustedCapacity=" + plan.trustedCapacityState()
                        + "|trustedCandidates=" + trustedCandidateFingerprint
        );
        boolean complete = diagnosticPolicyObservationRetained
                && itemDecisions.stream().allMatch(AutoDepositPolicyItemSnapshot::observationComplete);
        String missingBoundaries = diagnosticPolicyObservationRetained
                ? itemDecisions.stream()
                        .filter(item -> !item.observationComplete())
                        .map(item -> item.itemDecisionId() + "[" + item.missingBoundaries() + "]")
                        .reduce("NONE", (left, right) -> left.equals("NONE") ? right : left + "|" + right)
                : "POLICY_DECISIONS_NOT_RETAINED_DIAGNOSTICS_OFF";
        return new AutoDepositPolicySnapshot(
                inventorySnapshotId,
                autoPlanId,
                epoch,
                automaticContext,
                status,
                reason,
                occupied,
                total,
                plan.targetReliefSlots(),
                plan.expectedFreedSlots(),
                generalTargets,
                trustedTargets,
                aggregateTargets,
                destinationDecision,
                plan.trustedDestination().map(ChatClefDiagnostics::blockPos).orElse("NONE"),
                plan.trustedCandidates().size(),
                itemDecisions,
                complete,
                missingBoundaries
        );
    }

    public Object[] requiredFields() {
        return new Object[]{
                "inventorySnapshotId", inventorySnapshotId,
                "autoPlanId", autoPlanId,
                "autoOperationEpoch", automaticContext.available()
                        ? automaticContext.autoOperationEpoch()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "autoOperationId", automaticContext.available()
                        ? automaticContext.autoOperationId()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "maintenanceGenerationId", automaticContext.available()
                        ? automaticContext.maintenanceGenerationId()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "pressureOwnedRunId", automaticContext.available()
                        ? automaticContext.pressureOwnedRunId()
                        : "UNAVAILABLE_AT_POLICY_BOUNDARY",
                "policyContextEpoch", policyContextEpoch < 0 ? "UNAVAILABLE" : policyContextEpoch,
                "planningStatus", planningStatus,
                "planningReason", planningReason,
                "occupiedSlots", occupiedSlots < 0 ? "UNAVAILABLE" : occupiedSlots,
                "totalSlots", totalSlots < 0 ? "UNAVAILABLE" : totalSlots,
                "inventoryPressureState", occupiedSlots < 0 || totalSlots < 0
                        ? "UNAVAILABLE"
                        : occupiedSlots + "/" + totalSlots,
                "targetReliefSlots", targetReliefSlots < 0 ? "UNAVAILABLE" : targetReliefSlots,
                "expectedFreedSlots", expectedFreedSlots < 0 ? "UNAVAILABLE" : expectedFreedSlots,
                "selectedGeneralTargets", selectedGeneralTargets,
                "selectedTrustedTargets", selectedTrustedTargets,
                "selectedAggregateTargets", selectedAggregateTargets,
                "destinationDecision", destinationDecision,
                "trustedDestination", trustedDestination,
                "trustedCandidateCount", trustedCandidateCount,
                "policyItemDecisionCount", itemDecisions.size(),
                "policyPhysicalStackFactCount", itemDecisions.stream()
                        .mapToInt(item -> item.physicalStacks().size())
                        .sum(),
                "observationComplete", observationComplete,
                "missingBoundaries", missingBoundaries,
                "behavior_effect", "none"
        };
    }

    public String inventoryPressureState() {
        return occupiedSlots < 0 || totalSlots < 0
                ? "UNAVAILABLE"
                : occupiedSlots + "/" + totalSlots;
    }

    public Object[] optionalFields() {
        return new Object[0];
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }

    private static String diagnosticId(String kind, String semanticContent) {
        return "policy-" + kind + "-" + UUID.nameUUIDFromBytes(
                semanticContent.getBytes(StandardCharsets.UTF_8)
        );
    }
}
