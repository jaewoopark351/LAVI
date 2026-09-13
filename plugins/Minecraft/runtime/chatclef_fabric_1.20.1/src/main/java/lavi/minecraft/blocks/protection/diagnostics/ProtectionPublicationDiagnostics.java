//#if MC == 12001
package lavi.minecraft.blocks.protection.diagnostics;

import lavi.minecraft.blocks.protection.state.ProtectionSnapshot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//20260913_kpopmodder: Passive bounded publication observations never decide block protection or scheduling.
public final class ProtectionPublicationDiagnostics {
    private int emitted;
    private String previous = "";
    private java.lang.ref.WeakReference<Object> lastWorld = new java.lang.ref.WeakReference<>(null);
    private java.lang.ref.WeakReference<Object> lastPlayer = new java.lang.ref.WeakReference<>(null);
    private long observedLifetime;
    public void observe(ProtectionSnapshot snapshot, int positions, long elapsedNanos) {
        try {
            ChatClefDiagnostics.runIfDiagnosticsEligible(() -> observeEligible(snapshot, positions, elapsedNanos));
        } catch (RuntimeException | LinkageError ignored) { /* diagnostics cannot affect the published decision */ }
    }
    private void observeEligible(ProtectionSnapshot snapshot, int positions, long elapsedNanos) {
        if (lastWorld.get() != snapshot.world() || lastPlayer.get() != snapshot.player()) {
            observedLifetime++;
            lastWorld = new java.lang.ref.WeakReference<>(snapshot.world());
            lastPlayer = new java.lang.ref.WeakReference<>(snapshot.player());
        }
        String signature = ChatClefDiagnostics.diagnosticActivationEpoch() + ":" + observedLifetime + ":"
                + snapshot.status() + ":" + snapshot.pendingIndicators().size();
        if (emitted >= 64 || signature.equals(previous)) return;
        previous = signature; emitted++;
        try {
            ChatClefDiagnostics.logBoundedBoundaryWithDispatchResult("BLOCK_PROTECTION_PUBLICATION_BOUNDARY", snapshot.status(), null,
                    4096, new Object[]{
                    "worldIdentity", snapshot.world() == null ? "NONE" : Integer.toHexString(System.identityHashCode(snapshot.world())),
                    "observedWorldLifetime", observedLifetime,
                    "sourceRevision", snapshot.sourceRevision(), "protectionRevision", snapshot.revision(),
                    "protectedCount", snapshot.protectedBlocks().size(), "pendingRegionCount", snapshot.pendingIndicators().size(),
                    "positionsVisited", positions, "positionBudget", 4096, "elapsedNanos", elapsedNanos,
                    "localAttempt", emitted, "localAttemptLimit", 64, "filePersistence", "NOT_VERIFIED"}, new Object[0]);
        } catch (RuntimeException | LinkageError ignored) { /* diagnostics cannot affect the published decision */ }
    }
}

//#endif
