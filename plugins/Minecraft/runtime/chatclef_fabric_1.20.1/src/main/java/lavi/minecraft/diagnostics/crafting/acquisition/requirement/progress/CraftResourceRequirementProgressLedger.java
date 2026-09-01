package lavi.minecraft.diagnostics.crafting.acquisition.requirement.progress;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//20260901_kpopmodder: Deduplicate semantic requirement changes with bounded history.
public final class CraftResourceRequirementProgressLedger {
    private static final int HISTORY_LIMIT = 8;
    private final List<String> history = new ArrayList<>(HISTORY_LIMIT);
    private CraftResourceStage resourceStage = CraftResourceStage.UNKNOWN;
    private String activeRequirementItem = "UNAVAILABLE";
    private long activeRequirementCount = -1L;
    private long requirementTransitionCount;
    private long omittedRequirementHistoryCount;
    private long firstObservedTick = -1L;
    private long lastObservedTick = -1L;
    private long suppressedDetailCount;
    private long unownedObservationCount;
    private long unknownAssociationCount;
    private boolean counterSaturated;

    public synchronized boolean observe(
            CraftResourceRequirementProgressObservation observation) {
        Objects.requireNonNull(observation, "observation");
        if (observation.associationStatus()
                != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            accountUnowned(observation.associationStatus());
            return false;
        }
        retainTick(observation.observedTick());
        if (!observation.sourceEmissionCompleted()) {
            suppressedDetailCount = increment(suppressedDetailCount);
        }

        String nextItem = "UNAVAILABLE".equals(observation.activeRequirementItem())
                ? activeRequirementItem
                : observation.activeRequirementItem();
        long nextCount = observation.activeRequirementCount() < 0L
                ? activeRequirementCount
                : observation.activeRequirementCount();
        boolean changed = requirementTransitionCount == 0L
                || resourceStage != observation.resourceStage()
                || !activeRequirementItem.equals(nextItem)
                || activeRequirementCount != nextCount;
        if (!changed) {
            return false;
        }
        resourceStage = observation.resourceStage();
        activeRequirementItem = nextItem;
        activeRequirementCount = nextCount;
        requirementTransitionCount = increment(requirementTransitionCount);
        retainHistory(observation.sourceEventName());
        return true;
    }

    public synchronized CraftResourceRequirementProgressSnapshot snapshot() {
        return new CraftResourceRequirementProgressSnapshot(
                requirementTransitionCount,
                resourceStage,
                activeRequirementItem,
                activeRequirementCount,
                history,
                omittedRequirementHistoryCount,
                firstObservedTick,
                lastObservedTick,
                suppressedDetailCount,
                unownedObservationCount,
                unknownAssociationCount,
                counterSaturated
        );
    }

    private void retainTick(long tick) {
        if (tick < 0L) {
            return;
        }
        if (firstObservedTick < 0L) {
            firstObservedTick = tick;
        }
        lastObservedTick = tick;
    }

    private void retainHistory(String sourceEventName) {
        String sample = requirementTransitionCount + "|" + resourceStage.name()
                + "|" + activeRequirementItem + "|" + activeRequirementCount
                + "|" + sourceEventName;
        if (history.size() < HISTORY_LIMIT) {
            history.add(sample);
        } else {
            omittedRequirementHistoryCount = increment(omittedRequirementHistoryCount);
        }
    }

    private void accountUnowned(CraftResourceAssociationStatus status) {
        if (status == CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED) {
            unownedObservationCount = increment(unownedObservationCount);
        } else {
            unknownAssociationCount = increment(unknownAssociationCount);
        }
    }

    private long increment(long value) {
        if (value == Long.MAX_VALUE) {
            counterSaturated = true;
            return Long.MAX_VALUE;
        }
        return value + 1L;
    }
}
