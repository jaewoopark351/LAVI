package lavi.minecraft.task.container.deposit.auto.policy.diagnostics;

import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;

import java.util.ArrayList;
import java.util.List;

//20260830_kpopmodder: Freeze only per-item facts already computed by the automatic deposit policy.
public record AutoDepositPolicyItemSnapshot(String itemDecisionId,
                                            String itemId,
                                            int currentCount,
                                            String protectedCount,
                                            String workingSetCount,
                                            String workingSetReason,
                                            String categoryReserveCount,
                                            String categoryReserveReason,
                                            String computedSurplus,
                                            List<Integer> eligibleWholeStackCounts,
                                            List<Integer> selectedGeneralTargetCounts,
                                            List<Integer> selectedTrustedTargetCounts,
                                            String disposition,
                                            String classification,
                                            String destinationClass,
                                            String classificationConfidence,
                                            String classificationProvenance,
                                            List<AutoDepositPolicyStackSnapshot> physicalStacks,
                                            String decision,
                                            String decisionReason) {
    public AutoDepositPolicyItemSnapshot {
        itemDecisionId = normalize(itemDecisionId);
        itemId = normalize(itemId);
        protectedCount = normalize(protectedCount);
        workingSetCount = normalize(workingSetCount);
        workingSetReason = normalize(workingSetReason);
        categoryReserveCount = normalize(categoryReserveCount);
        categoryReserveReason = normalize(categoryReserveReason);
        computedSurplus = normalize(computedSurplus);
        eligibleWholeStackCounts = immutableCounts(eligibleWholeStackCounts);
        selectedGeneralTargetCounts = immutableCounts(selectedGeneralTargetCounts);
        selectedTrustedTargetCounts = immutableCounts(selectedTrustedTargetCounts);
        disposition = normalize(disposition);
        classification = normalize(classification);
        destinationClass = normalize(destinationClass);
        classificationConfidence = normalize(classificationConfidence);
        classificationProvenance = normalize(classificationProvenance);
        physicalStacks = physicalStacks == null ? List.of() : List.copyOf(physicalStacks);
        decision = normalize(decision);
        decisionReason = normalize(decisionReason);
    }

    public static AutoDepositPolicyItemSnapshot pending(String itemDecisionId,
                                                        String itemId,
                                                        int currentCount,
                                                        String protectedCount,
                                                        String workingSetCount,
                                                        String workingSetReason,
                                                        String categoryReserveCount,
                                                        String categoryReserveReason,
                                                        String computedSurplus,
                                                        List<Integer> eligibleWholeStackCounts,
                                                        String disposition,
                                                        String classification,
                                                        String destinationClass) {
        return pending(
                itemDecisionId,
                itemId,
                currentCount,
                protectedCount,
                workingSetCount,
                workingSetReason,
                categoryReserveCount,
                categoryReserveReason,
                computedSurplus,
                eligibleWholeStackCounts,
                disposition,
                classification,
                destinationClass,
                "UNAVAILABLE_NOT_RETAINED",
                "UNAVAILABLE_NOT_RETAINED",
                List.of()
        );
    }

    public static AutoDepositPolicyItemSnapshot pending(String itemDecisionId,
                                                        String itemId,
                                                        int currentCount,
                                                        String protectedCount,
                                                        String workingSetCount,
                                                        String workingSetReason,
                                                        String categoryReserveCount,
                                                        String categoryReserveReason,
                                                        String computedSurplus,
                                                        List<Integer> eligibleWholeStackCounts,
                                                        String disposition,
                                                        String classification,
                                                        String destinationClass,
                                                        String classificationConfidence,
                                                        String classificationProvenance,
                                                        List<AutoDepositPolicyStackSnapshot> physicalStacks) {
        return new AutoDepositPolicyItemSnapshot(
                itemDecisionId,
                itemId,
                currentCount,
                protectedCount,
                workingSetCount,
                workingSetReason,
                categoryReserveCount,
                categoryReserveReason,
                computedSurplus,
                eligibleWholeStackCounts,
                List.of(),
                List.of(),
                disposition,
                classification,
                destinationClass,
                classificationConfidence,
                classificationProvenance,
                physicalStacks,
                "PENDING_PLAN_FINALIZATION",
                "PENDING_PLAN_FINALIZATION"
        );
    }

    public AutoDepositPolicyItemSnapshot finalizeSelection(List<Integer> selectedGeneral,
                                                           List<Integer> selectedTrusted,
                                                           boolean trustedCandidateAvailable) {
        List<Integer> frozenGeneral = immutableCounts(selectedGeneral);
        List<Integer> frozenTrusted = immutableCounts(selectedTrusted);
        int selectedStepCount = frozenGeneral.size() + frozenTrusted.size();
        String finalDecision;
        String finalReason;
        if (selectedStepCount > 0) {
            finalDecision = "SELECTED";
            finalReason = selectedStepCount < eligibleWholeStackCounts.size()
                    ? "PARTIAL_SELECTION_RELIEF_LIMIT"
                    : "ALL_ELIGIBLE_STEPS_SELECTED";
        } else if ("NONE".equals(destinationClass)) {
            finalDecision = "EXCLUDED";
            finalReason = exclusionReason();
        } else if ("TRUSTED_ONLY".equals(destinationClass)
                && !eligibleWholeStackCounts.isEmpty()
                && !trustedCandidateAvailable) {
            finalDecision = "DEFERRED";
            finalReason = "NO_TRUSTED_CANDIDATE";
        } else if (!eligibleWholeStackCounts.isEmpty()) {
            finalDecision = "DEFERRED";
            finalReason = "RELIEF_LIMIT_OR_PRIORITY";
        } else {
            finalDecision = "EXCLUDED";
            finalReason = exclusionReason();
        }
        return new AutoDepositPolicyItemSnapshot(
                itemDecisionId,
                itemId,
                currentCount,
                protectedCount,
                workingSetCount,
                workingSetReason,
                categoryReserveCount,
                categoryReserveReason,
                computedSurplus,
                eligibleWholeStackCounts,
                frozenGeneral,
                frozenTrusted,
                disposition,
                classification,
                destinationClass,
                classificationConfidence,
                classificationProvenance,
                physicalStacks,
                finalDecision,
                finalReason
        );
    }

    public int selectedAggregateTargetCount() {
        return selectedGeneralTargetCounts.stream().mapToInt(Integer::intValue).sum()
                + selectedTrustedTargetCounts.stream().mapToInt(Integer::intValue).sum();
    }

    public String summary() {
        return itemDecisionId
                + "{item=" + itemId
                + ",current=" + currentCount
                + ",protected=" + protectedCount
                + ",working=" + workingSetCount
                + ",workingReason=" + workingSetReason
                + ",reserve=" + categoryReserveCount
                + ",reserveReason=" + categoryReserveReason
                + ",surplus=" + computedSurplus
                + ",eligible=" + eligibleWholeStackCounts
                + ",general=" + selectedGeneralTargetCounts
                + ",trusted=" + selectedTrustedTargetCounts
                + ",aggregate=" + selectedAggregateTargetCount()
                + ",disposition=" + disposition
                + ",classification=" + classification
                + ",classificationConfidence=" + classificationConfidence
                + ",classificationProvenance=" + classificationProvenance
                + ",destination=" + destinationClass
                + ",physicalStacks=" + physicalStacks.stream()
                        .map(AutoDepositPolicyStackSnapshot::summary)
                        .toList()
                + ",decision=" + decision
                + ",reason=" + decisionReason
                + "}";
    }

    public Object[] requiredFields(String inventorySnapshotId,
                                   String autoPlanId,
                                   long policyContextEpoch,
                                   StoreDepositAutomaticContext automaticContext) {
        StoreDepositAutomaticContext context = automaticContext == null
                ? StoreDepositAutomaticContext.unavailable()
                : automaticContext;
        boolean complete = observationComplete();
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
                "itemId", itemId,
                "currentCount", currentCount,
                "protectedCount", protectedCount,
                "workingSetCount", workingSetCount,
                "workingSetReason", workingSetReason,
                "categoryReserveCount", categoryReserveCount,
                "categoryReserveReason", categoryReserveReason,
                "computedSurplus", computedSurplus,
                "eligibleWholeStackCounts", eligibleWholeStackCounts,
                "selectedGeneralTargetCounts", selectedGeneralTargetCounts,
                "selectedTrustedTargetCounts", selectedTrustedTargetCounts,
                "selectedAggregateTargetCount", selectedAggregateTargetCount(),
                "disposition", disposition,
                "classification", classification,
                "classificationConfidence", classificationConfidence,
                "classificationProvenance", classificationProvenance,
                "destinationClass", destinationClass,
                "physicalStackFactCount", physicalStacks.size(),
                "physicalStackFactIds", physicalStacks.stream()
                        .map(AutoDepositPolicyStackSnapshot::stackFactId)
                        .toList(),
                "decision", decision,
                "decisionReason", decisionReason,
                "observationComplete", complete,
                "missingBoundaries", missingBoundaries(),
                "behavior_effect", "none"
        };
    }

    public boolean observationComplete() {
        return "NONE".equals(missingBoundaries());
    }

    public String missingBoundaries() {
        List<String> missing = new ArrayList<>();
        if (physicalStacks.isEmpty()) {
            missing.add("PHYSICAL_STACK_FACTS");
        }
        if (workingSetReason.startsWith("UNAVAILABLE")) {
            missing.add("WORKING_SET_RESERVATION_PROVENANCE");
        }
        if (categoryReserveReason.startsWith("UNAVAILABLE")) {
            missing.add("CATEGORY_RESERVE_REASON");
        }
        if (classification.startsWith("UNAVAILABLE")) {
            missing.add("CLASSIFICATION");
        }
        if (classificationConfidence.startsWith("UNAVAILABLE")) {
            missing.add("CLASSIFICATION_CONFIDENCE");
        }
        if (classificationProvenance.startsWith("UNAVAILABLE")) {
            missing.add("CLASSIFICATION_PROVENANCE");
        }
        return missing.isEmpty() ? "NONE" : String.join(",", missing);
    }

    private String exclusionReason() {
        if ("NOT_EVALUATED_HARD_PROTECTION".equals(computedSurplus)) {
            return "HARD_PROTECTED_SHORT_CIRCUIT";
        }
        if (("DEPOSITABLE_SURPLUS".equals(disposition)
                || "CONDITIONAL_VALUABLE".equals(disposition))
                && eligibleWholeStackCounts.isEmpty()) {
            return "NO_WHOLE_STACK_TRANSFER_STEP";
        }
        return disposition;
    }

    private static List<Integer> immutableCounts(List<Integer> counts) {
        return counts == null
                ? List.of()
                : counts.stream()
                        .filter(value -> value != null && value > 0)
                        .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
