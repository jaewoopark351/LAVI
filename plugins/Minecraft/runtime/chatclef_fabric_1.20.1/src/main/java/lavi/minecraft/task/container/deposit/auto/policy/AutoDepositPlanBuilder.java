package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.Dimension;
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

        Map<Item, List<AutoDepositStackSnapshot>> grouped = new LinkedHashMap<>();
        for (AutoDepositStackSnapshot stack : stacks) {
            if (stack.isMainInventory()) {
                grouped.computeIfAbsent(stack.item(), ignored -> new ArrayList<>()).add(stack);
            }
        }

        List<AutoDepositPlannedItem> general = new ArrayList<>();
        List<AutoDepositPlannedItem> conditional = new ArrayList<>();
        Map<Item, Integer> protectedCounts = new LinkedHashMap<>();
        Map<Item, AutoDepositDisposition> dispositions = new LinkedHashMap<>();
        List<String> semantics = new ArrayList<>(context.taskPathFingerprint());
        stacks.stream().map(AutoDepositStackSnapshot::semanticIdentity).sorted().forEach(semantics::add);

        List<Map.Entry<Item, List<AutoDepositStackSnapshot>>> groups = new ArrayList<>(grouped.entrySet());
        groups.sort(Comparator.comparing(entry -> entry.getValue().get(0).itemId()));
        for (Map.Entry<Item, List<AutoDepositStackSnapshot>> entry : groups) {
            Item item = entry.getKey();
            List<AutoDepositStackSnapshot> itemStacks = entry.getValue();
            AutoDepositStackSnapshot representative = itemStacks.get(0);
            int currentCount = itemStacks.stream().mapToInt(AutoDepositStackSnapshot::count).sum();

            if (hardProtection.hasProtectedStackOf(item)) {
                protectedCounts.put(item, currentCount);
                dispositions.put(item, AutoDepositDisposition.HARD_PROTECTED);
                semantics.add(itemSemantic(representative.itemId(), "hard", 0, itemStacks.size()));
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
                continue;
            }

            List<Integer> wholeStackSteps = wholeStackTransferSteps(itemStacks, protectedCount);
            AutoDepositItemClass itemClass = classification.classify(representative);
            switch (itemClass) {
                case GENERAL_SURPLUS -> {
                    addSteps(general, item, representative.itemId(), wholeStackSteps);
                    dispositions.put(item, AutoDepositDisposition.DEPOSITABLE_SURPLUS);
                }
                case CONDITIONAL_VALUABLE, RARE_FUNCTIONAL -> {
                    addSteps(conditional, item, representative.itemId(), wholeStackSteps);
                    dispositions.put(item, AutoDepositDisposition.CONDITIONAL_VALUABLE);
                }
                case UNKNOWN -> dispositions.put(item, AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE);
            }
            semantics.add(itemSemantic(
                    representative.itemId(), dispositions.get(item).name(), wholeStackSteps.size(), itemStacks.size()
            ));
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
