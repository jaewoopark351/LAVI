package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

//20260907_kpopmodder: Evaluate immutable before/after GET evidence without mutating command state.
final class FabricChatClefGetItemEffectEvidence {
    private final FabricChatClefGetItemEffectProfile profile;
    private final FabricChatClefGetItemCountObservation before;
    private final FabricChatClefGetItemCountObservation after;
    private final String observationStatus;
    private final String observationReason;
    private final Integer delta;

    private FabricChatClefGetItemEffectEvidence(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemCountObservation before,
            FabricChatClefGetItemCountObservation after,
            String observationStatus,
            String observationReason,
            Integer delta
    ) {
        this.profile = profile;
        this.before = before;
        this.after = after;
        this.observationStatus = observationStatus;
        this.observationReason = observationReason;
        this.delta = delta;
    }

    static FabricChatClefGetItemEffectEvidence evaluate(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemCountObservation before,
            FabricChatClefGetItemCountObservation after
    ) {
        if (!before.authoritative() && !after.authoritative()) {
            return unavailable(
                    profile,
                    before,
                    after,
                    "unavailable",
                    "before=" + before.unavailableReason()
                            + ",after=" + after.unavailableReason()
            );
        }
        if (!before.authoritative()) {
            return unavailable(
                    profile,
                    before,
                    after,
                    "before_unavailable",
                    before.unavailableReason()
            );
        }
        if (!after.authoritative()) {
            return unavailable(
                    profile,
                    before,
                    after,
                    "after_unavailable",
                    after.unavailableReason()
            );
        }
        if (before.worldIdentity() != after.worldIdentity()
                || before.playerIdentity() != after.playerIdentity()) {
            return unavailable(
                    profile,
                    before,
                    after,
                    "stale_world_binding",
                    "world_or_player_identity_changed"
            );
        }
        return new FabricChatClefGetItemEffectEvidence(
                profile,
                before,
                after,
                "authoritative",
                "before_and_after_inventory_counts_match_world_binding",
                after.countOrNull() - before.countOrNull()
        );
    }

    private static FabricChatClefGetItemEffectEvidence unavailable(
            FabricChatClefGetItemEffectProfile profile,
            FabricChatClefGetItemCountObservation before,
            FabricChatClefGetItemCountObservation after,
            String status,
            String reason
    ) {
        return new FabricChatClefGetItemEffectEvidence(
                profile,
                before,
                after,
                status,
                reason,
                null
        );
    }

    FabricChatClefGetItemEffectProfile profile() {
        return profile;
    }

    Integer beforeCountOrNull() {
        return before.countOrNull();
    }

    Integer afterCountOrNull() {
        return after.countOrNull();
    }

    Integer deltaOrNull() {
        return delta;
    }

    String observationStatus() {
        return observationStatus;
    }

    String observationReason() {
        return observationReason;
    }
}
