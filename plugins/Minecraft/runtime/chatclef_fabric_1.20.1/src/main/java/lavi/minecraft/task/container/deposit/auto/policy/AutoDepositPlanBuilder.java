package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyStackSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class AutoDepositPlanBuilder {
    private final AutoDepositPolicyDefinition definition;
    private final AutoDepositItemClassificationPolicy classification;

    public AutoDepositPlanBuilder(AutoDepositPolicyDefinition definition,
                                  AutoDepositItemClassificationPolicy classification) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.classification = Objects.requireNonNull(classification, "classification");
    }

    public AutoDepositPlanDraft prepare(AutoDepositContextSnapshot context,
                                        List<AutoDepositStackSnapshot> stacks,
                                        AutoDepositHardProtectionResult hardProtection,
                                        Map<Item, Integer> categoryReserves,
                                        int startingOccupiedSlots,
                                        int targetReliefSlots) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(stacks, "stacks");
        Objects.requireNonNull(hardProtection, "hardProtection");
        Objects.requireNonNull(categoryReserves, "categoryReserves");

        boolean diagnosticsEnabled = ChatClefDiagnostics.isBoundaryEnabled();
        Map<Item, List<AutoDepositStackSnapshot>> grouped = new LinkedHashMap<>();
        Map<Item, List<AutoDepositStackSnapshot>> allPhysicalStacksByItem = diagnosticsEnabled
                ? new LinkedHashMap<>()
                : Map.of();
        for (AutoDepositStackSnapshot stack : stacks) {
            if (diagnosticsEnabled) {
                allPhysicalStacksByItem.computeIfAbsent(
                        stack.item(), ignored -> new ArrayList<>()
                ).add(stack);
            }
            if (stack.isMainInventory()) {
                grouped.computeIfAbsent(stack.item(), ignored -> new ArrayList<>()).add(stack);
            }
        }

        List<AutoDepositPlannedItem> general = new ArrayList<>();
        List<AutoDepositPlannedItem> conditional = new ArrayList<>();
        Map<Item, Integer> protectedCounts = new LinkedHashMap<>();
        Map<Item, AutoDepositDisposition> dispositions = new LinkedHashMap<>();
        List<AutoDepositPolicyItemSnapshot> policyItemDecisions = diagnosticsEnabled
                ? new ArrayList<>()
                : List.of();
        String diagnosticInventoryFingerprint = diagnosticsEnabled
                ? diagnosticInventoryFingerprint(stacks)
                : "UNAVAILABLE_DIAGNOSTICS_OFF";
        Set<AutoDepositStackSnapshot> protectedDiagnosticStacks = diagnosticsEnabled
                ? Set.copyOf(hardProtection.protectedStacks())
                : Set.of();
        List<String> semantics = new ArrayList<>(context.taskPathFingerprint());
        stacks.stream().map(AutoDepositStackSnapshot::semanticIdentity).sorted().forEach(semantics::add);

        List<Map.Entry<Item, List<AutoDepositStackSnapshot>>> groups = new ArrayList<>(grouped.entrySet());
        groups.sort(Comparator.comparing(entry -> entry.getValue().get(0).itemId()));
        int itemOrdinal = 0;
        for (Map.Entry<Item, List<AutoDepositStackSnapshot>> entry : groups) {
            Item item = entry.getKey();
            List<AutoDepositStackSnapshot> itemStacks = entry.getValue();
            AutoDepositStackSnapshot representative = itemStacks.get(0);
            int currentCount = itemStacks.stream().mapToInt(AutoDepositStackSnapshot::count).sum();
            String itemDecisionId = diagnosticsEnabled
                    ? "policy-" + context.epoch() + "-item-" + itemOrdinal++
                    : "UNAVAILABLE_DIAGNOSTICS_OFF";
            List<AutoDepositPolicyStackSnapshot> physicalStacks = diagnosticsEnabled
                    ? policyStackSnapshots(
                            itemDecisionId,
                            allPhysicalStacksByItem.getOrDefault(item, itemStacks),
                            protectedDiagnosticStacks
                    )
                    : List.of();

            if (hardProtection.hasProtectedStackOf(item)) {
                protectedCounts.put(item, currentCount);
                dispositions.put(item, AutoDepositDisposition.HARD_PROTECTED);
                semantics.add(itemSemantic(representative.itemId(), "hard", 0, itemStacks.size()));
                if (diagnosticsEnabled) {
                    policyItemDecisions.add(AutoDepositPolicyItemSnapshot.pending(
                        itemDecisionId,
                        representative.itemId(),
                        currentCount,
                        Integer.toString(currentCount),
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        List.of(),
                        AutoDepositDisposition.HARD_PROTECTED.name(),
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NONE",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        "NOT_EVALUATED_HARD_PROTECTION",
                        physicalStacks
                    ));
                }
                continue;
            }

            int working = context.workingSet() == null ? 0 : context.workingSet().reservedCount(item);
            int reserve = categoryReserves.getOrDefault(item, 0);
            int protectedCount = Math.min(currentCount, Math.max(working, reserve));
            if (protectedCount > 0) {
                protectedCounts.put(item, protectedCount);
            }
            int surplus = Math.max(0, currentCount - protectedCount);
            if (surplus == 0) {
                AutoDepositDisposition disposition = protectedDisposition(working, reserve);
                dispositions.put(item, disposition);
                semantics.add(itemSemantic(representative.itemId(), disposition.name(), 0, itemStacks.size()));
                if (diagnosticsEnabled) {
                    policyItemDecisions.add(AutoDepositPolicyItemSnapshot.pending(
                        itemDecisionId,
                        representative.itemId(),
                        currentCount,
                        Integer.toString(protectedCount),
                        Integer.toString(working),
                        workingSetReason(context),
                        Integer.toString(reserve),
                        categoryReserveReason(reserve),
                        Integer.toString(surplus),
                        List.of(),
                        disposition.name(),
                        "NOT_EVALUATED_NO_SURPLUS",
                        "NONE",
                        "NOT_EVALUATED_NO_SURPLUS",
                        "NOT_EVALUATED_NO_SURPLUS",
                        physicalStacks
                    ));
                }
                continue;
            }

            List<Integer> wholeStackSteps = wholeStackTransferSteps(itemStacks, protectedCount);
            AutoDepositItemClass itemClass = classification.classify(representative);
            String destinationClass = "NONE";
            switch (itemClass) {
                case GENERAL_SURPLUS -> {
                    addSteps(general, item, representative.itemId(), wholeStackSteps);
                    dispositions.put(item, AutoDepositDisposition.DEPOSITABLE_SURPLUS);
                    destinationClass = "GENERAL_CONTAINER";
                }
                case CONDITIONAL_VALUABLE, RARE_FUNCTIONAL -> {
                    addSteps(conditional, item, representative.itemId(), wholeStackSteps);
                    dispositions.put(item, AutoDepositDisposition.CONDITIONAL_VALUABLE);
                    destinationClass = "TRUSTED_ONLY";
                }
                case UNKNOWN -> {
                    dispositions.put(item, AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE);
                    destinationClass = "NONE";
                }
            }
            semantics.add(itemSemantic(
                    representative.itemId(), dispositions.get(item).name(), wholeStackSteps.size(), itemStacks.size()
            ));
            if (diagnosticsEnabled) {
                policyItemDecisions.add(AutoDepositPolicyItemSnapshot.pending(
                    itemDecisionId,
                    representative.itemId(),
                    currentCount,
                    Integer.toString(protectedCount),
                    Integer.toString(working),
                    workingSetReason(context),
                    Integer.toString(reserve),
                    categoryReserveReason(reserve),
                    Integer.toString(surplus),
                    wholeStackSteps,
                    dispositions.get(item).name(),
                    itemClass.name(),
                    destinationClass,
                    "UNAVAILABLE_NOT_COMPUTED_BY_POLICY",
                    "UNAVAILABLE_DECISION_BRANCH_NOT_RETAINED",
                    physicalStacks
                ));
            }
        }

        if (diagnosticsEnabled) {
            List<Map.Entry<Item, List<AutoDepositStackSnapshot>>> nonMainOnlyGroups =
                    allPhysicalStacksByItem.entrySet().stream()
                            .filter(entry -> !grouped.containsKey(entry.getKey()))
                            .sorted(Comparator.comparing(entry -> entry.getValue().get(0).itemId()))
                            .toList();
            for (Map.Entry<Item, List<AutoDepositStackSnapshot>> entry : nonMainOnlyGroups) {
                List<AutoDepositStackSnapshot> itemStacks = entry.getValue();
                AutoDepositStackSnapshot representative = itemStacks.get(0);
                String itemDecisionId = "policy-" + context.epoch() + "-item-" + itemOrdinal++;
                policyItemDecisions.add(AutoDepositPolicyItemSnapshot.pending(
                        itemDecisionId,
                        representative.itemId(),
                        itemStacks.stream().mapToInt(AutoDepositStackSnapshot::count).sum(),
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        List.of(),
                        "EXCLUDED_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NONE",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        "NOT_APPLICABLE_NON_MAIN_INVENTORY",
                        policyStackSnapshots(
                                itemDecisionId,
                                itemStacks,
                                protectedDiagnosticStacks
                        )
                ));
            }
        }

        Comparator<AutoDepositPlannedItem> priority = Comparator
                .comparingInt(AutoDepositPlannedItem::expectedFreedSlots).reversed()
                .thenComparing(Comparator.comparingInt(AutoDepositPlannedItem::count).reversed())
                .thenComparing(AutoDepositPlannedItem::itemId);
        general.sort(priority);
        conditional.sort(priority);
        return new AutoDepositPlanDraft(
                context,
                general,
                conditional,
                protectedCounts,
                dispositions,
                policyItemDecisions,
                diagnosticInventoryFingerprint,
                diagnosticsEnabled,
                semantics,
                startingOccupiedSlots,
                targetReliefSlots
        );
    }

    public AutoDepositPlan finish(AutoDepositPlanDraft draft,
                                  Optional<BlockPos> trustedDestination,
                                  long trustedRevision,
                                  String trustedCapacityState) {
        Objects.requireNonNull(trustedDestination, "trustedDestination");
        List<AutoDepositTrustedDestinationCandidate> candidates = trustedDestination.isPresent()
                ? List.of(legacyCandidate(draft, trustedDestination.get()))
                : List.of();
        return finish(draft, candidates, trustedRevision, trustedCapacityState);
    }

    //20260827_kpopmodder: Route trusted-only targets only when a bounded candidate snapshot exists.
    public AutoDepositPlan finish(
            AutoDepositPlanDraft draft,
            List<AutoDepositTrustedDestinationCandidate> trustedCandidates,
            long trustedRevision,
            String trustedCapacityState) {
        Objects.requireNonNull(draft, "draft");
        Objects.requireNonNull(trustedCandidates, "trustedCandidates");
        Objects.requireNonNull(trustedCapacityState, "trustedCapacityState");

        List<AutoDepositPlannedItem> selectedGeneral = take(
                draft.generalItems(), draft.targetReliefSlots()
        );
        int remainingRelief = Math.max(0, draft.targetReliefSlots() - selectedGeneral.size());
        List<AutoDepositPlannedItem> selectedTrusted = !trustedCandidates.isEmpty()
                ? take(draft.conditionalItems(), remainingRelief)
                : List.of();
        ItemTarget[] generalTargets = toTargets(selectedGeneral);
        ItemTarget[] trustedTargets = toTargets(selectedTrusted);
        int expectedFreedSlots = selectedGeneral.size() + selectedTrusted.size();
        List<AutoDepositTrustedDestinationCandidate> effectiveTrustedCandidates =
                selectedTrusted.isEmpty() ? List.of() : List.copyOf(trustedCandidates);
        List<AutoDepositPolicyItemSnapshot> policyItemDecisions;
        if (draft.diagnosticPolicyObservationRetained()) {
            Map<String, List<Integer>> selectedGeneralCounts = selectedCounts(selectedGeneral);
            Map<String, List<Integer>> selectedTrustedCounts = selectedCounts(selectedTrusted);
            policyItemDecisions = draft.policyItemDecisions().stream()
                    .map(item -> item.finalizeSelection(
                            selectedGeneralCounts.getOrDefault(item.itemId(), List.of()),
                            selectedTrustedCounts.getOrDefault(item.itemId(), List.of()),
                            !trustedCandidates.isEmpty()
                    ))
                    .toList();
        } else {
            policyItemDecisions = List.of();
        }

        List<String> semantics = new ArrayList<>(draft.semanticEntries());
        semantics.add("trustedCandidates=" + effectiveTrustedCandidates.stream()
                .map(candidate -> candidate.destinationId() + ":" + candidate.observedState())
                .reduce("none", (left, right) -> left.equals("none") ? right : left + "," + right));
        semantics.add("targetReliefSlots=" + draft.targetReliefSlots());
        semantics.add("expectedFreedSlots=" + expectedFreedSlots);
        AutoDepositContextSnapshot context = draft.context();
        AutoDepositDecisionFingerprint fingerprint = new AutoDepositDecisionFingerprint(
                context.worldIdentity(),
                context.userTaskRoot(),
                context.dimension(),
                context.persistentWorldKey(),
                definition.revision(),
                trustedRevision,
                trustedCapacityState,
                semantics
        );
        return new AutoDepositPlan(
                context,
                generalTargets,
                trustedTargets,
                effectiveTrustedCandidates,
                draft.protectedCounts(),
                draft.dispositions(),
                policyItemDecisions,
                draft.diagnosticInventoryFingerprint(),
                trustedRevision,
                trustedCapacityState,
                draft.startingOccupiedSlots(),
                draft.targetReliefSlots(),
                expectedFreedSlots,
                fingerprint
        );
    }

    private static AutoDepositTrustedDestinationCandidate legacyCandidate(
            AutoDepositPlanDraft draft,
            BlockPos position) {
        AutoDepositContextSnapshot context = draft.context();
        Dimension dimension = context.dimension();
        AutoDepositTrustedDestination destination = new AutoDepositTrustedDestination(
                context.persistentWorldKey(),
                dimension,
                position,
                true
        );
        return new AutoDepositTrustedDestinationCandidate(
                destination, 0, 0.0, "legacy_explicit_candidate"
        );
    }

    private static AutoDepositDisposition protectedDisposition(int working, int reserve) {
        if (working >= reserve && working > 0) {
            return AutoDepositDisposition.WORKING_SET_RESERVED;
        }
        if (reserve > 0) {
            return AutoDepositDisposition.CATEGORY_RESERVED;
        }
        return AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE;
    }

    private static List<Integer> wholeStackTransferSteps(List<AutoDepositStackSnapshot> stacks,
                                                         int protectedCount) {
        List<Integer> counts = stacks.stream()
                .map(AutoDepositStackSnapshot::count)
                .sorted(Comparator.reverseOrder())
                .toList();
        int remainingCount = counts.stream().mapToInt(Integer::intValue).sum();
        List<Integer> result = new ArrayList<>();
        for (int count : counts) {
            if (remainingCount - count < protectedCount) {
                break;
            }
            result.add(count);
            remainingCount -= count;
        }
        return List.copyOf(result);
    }

    private static void addSteps(List<AutoDepositPlannedItem> output,
                                 Item item,
                                 String itemId,
                                 List<Integer> transferCounts) {
        transferCounts.forEach(count -> output.add(
                new AutoDepositPlannedItem(item, itemId, count, 1)
        ));
    }

    private static List<AutoDepositPlannedItem> take(List<AutoDepositPlannedItem> source,
                                                     int maximumSize) {
        if (maximumSize <= 0 || source.isEmpty()) {
            return List.of();
        }
        return List.copyOf(source.subList(0, Math.min(maximumSize, source.size())));
    }

    private static Map<String, List<Integer>> selectedCounts(List<AutoDepositPlannedItem> selected) {
        Map<String, List<Integer>> mutable = new LinkedHashMap<>();
        for (AutoDepositPlannedItem item : selected) {
            mutable.computeIfAbsent(item.itemId(), ignored -> new ArrayList<>()).add(item.count());
        }
        Map<String, List<Integer>> frozen = new LinkedHashMap<>();
        mutable.forEach((itemId, counts) -> frozen.put(itemId, List.copyOf(counts)));
        return Map.copyOf(frozen);
    }

    private static String workingSetReason(AutoDepositContextSnapshot context) {
        return context.workingSet() == null
                ? "NO_ACTIVE_WORKING_SET"
                : "UNAVAILABLE_RESERVATION_PROVENANCE_NOT_RETAINED";
    }

    private static List<AutoDepositPolicyStackSnapshot> policyStackSnapshots(
            String itemDecisionId,
            List<AutoDepositStackSnapshot> stacks,
            Set<AutoDepositStackSnapshot> protectedStacks) {
        List<AutoDepositPolicyStackSnapshot> result = new ArrayList<>(stacks.size());
        int stackOrdinal = 0;
        for (AutoDepositStackSnapshot stack : stacks) {
            result.add(AutoDepositPolicyStackSnapshot.capture(
                    itemDecisionId,
                    stackOrdinal++,
                    stack,
                    protectedStacks.contains(stack)
            ));
        }
        return List.copyOf(result);
    }

    private static String categoryReserveReason(int reserve) {
        return reserve > 0
                ? "UNAVAILABLE_ALLOCATION_REASON_NOT_RETAINED"
                : "NO_CATEGORY_RESERVE_ALLOCATED";
    }

    private static String diagnosticInventoryFingerprint(List<AutoDepositStackSnapshot> stacks) {
        List<String> facts = stacks.stream()
                .map(stack -> stack.semanticIdentity()
                        + ":count=" + stack.count()
                        + ":max=" + stack.maxCount()
                        + ":special=" + stack.specialMetadata()
                        + ":food=" + stack.food()
                        + ":block=" + stack.blockItem()
                        + ":falling=" + stack.fallingBlock())
                .sorted()
                .toList();
        return facts.isEmpty() ? "EMPTY" : String.join("|", facts);
    }

    private static ItemTarget[] toTargets(List<AutoDepositPlannedItem> items) {
        return items.stream()
                .map(item -> new ItemTarget(item.item(), item.count()))
                .toArray(ItemTarget[]::new);
    }

    private static String itemSemantic(String itemId,
                                       String disposition,
                                       int freeableSlots,
                                       int sourceStacks) {
        return "item=" + itemId + ":disposition=" + disposition
                + ":freeable=" + freeableSlots + ":stacks=" + sourceStacks;
    }
}
