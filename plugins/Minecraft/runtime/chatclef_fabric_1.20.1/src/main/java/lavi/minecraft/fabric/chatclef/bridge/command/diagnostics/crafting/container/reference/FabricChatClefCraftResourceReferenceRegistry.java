package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.container.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetTuple;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bound non-authoritative stage/role references independently from attempt history.
public final class FabricChatClefCraftResourceReferenceRegistry {
    private static final int ACTIVE_SCOPE_LIMIT = 8;
    private static final int DETAIL_TRANSITION_LIMIT = 64;

    private final Map<IronPickaxeAcquisitionScopeKey, FabricChatClefCraftResourceReferenceState>
            current =
            new LinkedHashMap<>();

    public synchronized FabricChatClefCraftResourceReferenceDecision observe(
            IronPickaxeAcquisitionScopeKey key,
            CraftResourceAssociationStatus associationStatus,
            CraftResourceTargetTuple observed) {
        FabricChatClefCraftResourceReferenceState state = key == null
                ? null
                : current.get(key);
        CraftResourceTargetTuple previous = state == null ? null : state.current();
        if (key == null
                || observed == null
                || associationStatus != CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT) {
            return decision(false, false, false, previous,
                    observed == null ? unavailableTuple() : observed, state);
        }
        if (observed.equals(previous)) {
            return decision(false, true, false, observed, observed, state);
        }
        if (previous == null && current.size() >= ACTIVE_SCOPE_LIMIT) {
            return decision(false, false, false, null, observed, null);
        }
        long transitionCount = increment(state == null ? 0L : state.transitionCount());
        boolean detailEligible = state == null
                || state.detailEligibleTransitionCount() < DETAIL_TRANSITION_LIMIT;
        long eligibleCount = detailEligible
                ? increment(state == null ? 0L : state.detailEligibleTransitionCount())
                : state.detailEligibleTransitionCount();
        long suppressedCount = detailEligible
                ? state == null ? 0L : state.suppressedDetailTransitionCount()
                : increment(state == null ? 0L : state.suppressedDetailTransitionCount());
        boolean saturated = (state != null && state.counterSaturated())
                || transitionCount == Long.MAX_VALUE
                || eligibleCount == Long.MAX_VALUE
                || suppressedCount == Long.MAX_VALUE;
        FabricChatClefCraftResourceReferenceState updated =
                new FabricChatClefCraftResourceReferenceState(
                        observed,
                        transitionCount,
                        eligibleCount,
                        suppressedCount,
                        saturated
                );
        current.put(key, updated);
        return decision(
                true,
                true,
                detailEligible,
                previous,
                observed,
                updated
        );
    }

    public synchronized void retire(IronPickaxeAcquisitionScopeKey key) {
        current.remove(key);
    }

    public synchronized void clearForModeOff() {
        current.clear();
    }

    private static CraftResourceTargetTuple unavailableTuple() {
        return new CraftResourceTargetTuple(
                lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage.UNKNOWN,
                lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole.UNKNOWN,
                "UNAVAILABLE",
                java.util.List.of()
        );
    }

    private static FabricChatClefCraftResourceReferenceDecision decision(
            boolean changed,
            boolean retained,
            boolean detailEligible,
            CraftResourceTargetTuple previous,
            CraftResourceTargetTuple observed,
            FabricChatClefCraftResourceReferenceState state) {
        return new FabricChatClefCraftResourceReferenceDecision(
                changed,
                retained,
                detailEligible,
                Optional.ofNullable(previous),
                observed,
                state == null ? 0L : state.transitionCount(),
                state == null ? 0L : state.detailEligibleTransitionCount(),
                state == null ? 0L : state.suppressedDetailTransitionCount(),
                state != null && state.counterSaturated()
        );
    }

    private static long increment(long value) {
        return value == Long.MAX_VALUE ? Long.MAX_VALUE : value + 1L;
    }
}
