package lavi.minecraft.diagnostics.container.store.deposit.candidate;

public enum StoreContainerCandidateRejectionReason {
    ACCEPTED,
    CHEST_ABOVE_BLOCKED_UNBREAKABLE,
    CONTAINER_CACHE_FULL,
    CACHED_DUNGEON_CHEST,
    SPAWNER_NEAR_CHEST,
    UNKNOWN;

    public boolean accepted() {
        return this == ACCEPTED;
    }
}
