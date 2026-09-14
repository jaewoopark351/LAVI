package lavi.minecraft.diagnostics.container.store.deposit.counter;

//20260914_kpopmodder: Preserve actual EventBus registration facts separately from diagnostic subscription flags.
public record StoreCounterSubscriptionSnapshot(
        boolean available, boolean registered, boolean pending, boolean deleteRequested,
        int registeredIndex, int registeredCount, int pendingCount, String unavailableReason) {
    public static StoreCounterSubscriptionSnapshot unavailable() {
        return unavailable("UNAVAILABLE_SOURCE_STATE");
    }
    public static StoreCounterSubscriptionSnapshot unavailable(String reason) {
        return new StoreCounterSubscriptionSnapshot(false, false, false, false, -1, -1, -1, reason);
    }

    public Object[] fields() {
        return new Object[]{
                "registrySnapshotAvailable", available, "registrySnapshotReason", available ? "OBSERVED" : unavailableReason,
                "nativeRegistered", available ? registered : "UNAVAILABLE",
                "nativePendingRegistration", available ? pending : "UNAVAILABLE",
                "nativeDeleteRequested", available ? deleteRequested : "UNAVAILABLE",
                "nativeEligibleNow", available ? registered && !deleteRequested : "UNAVAILABLE",
                "nativeRegisteredIndex", available ? registeredIndex : "UNAVAILABLE",
                "nativeRegisteredCount", available ? registeredCount : "UNAVAILABLE",
                "nativePendingCount", available ? pendingCount : "UNAVAILABLE",
                "subscriptionEvidence", "EVENTBUS_NATIVE_COLLECTIONS_AT_BOUNDARY"
        };
    }
}
