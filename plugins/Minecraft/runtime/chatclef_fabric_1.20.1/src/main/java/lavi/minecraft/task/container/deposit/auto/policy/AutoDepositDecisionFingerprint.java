package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.util.Dimension;

import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Represent only policy-relevant changes that may release a no-safe-surplus latch.
public final class AutoDepositDecisionFingerprint {
    private final Object worldIdentity;
    private final Object userTaskRoot;
    private final Dimension dimension;
    private final String persistentWorldKey;
    private final int policyRevision;
    private final long trustedRevision;
    private final String trustedCapacityState;
    private final List<String> semanticEntries;

    public AutoDepositDecisionFingerprint(Object worldIdentity,
                                          Object userTaskRoot,
                                          Dimension dimension,
                                          String persistentWorldKey,
                                          int policyRevision,
                                          long trustedRevision,
                                          String trustedCapacityState,
                                          List<String> semanticEntries) {
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
        this.userTaskRoot = userTaskRoot;
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.persistentWorldKey = Objects.requireNonNull(persistentWorldKey, "persistentWorldKey");
        this.policyRevision = policyRevision;
        this.trustedRevision = trustedRevision;
        this.trustedCapacityState = Objects.requireNonNull(trustedCapacityState, "trustedCapacityState");
        this.semanticEntries = List.copyOf(semanticEntries);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AutoDepositDecisionFingerprint fingerprint)) {
            return false;
        }
        return worldIdentity == fingerprint.worldIdentity
                && userTaskRoot == fingerprint.userTaskRoot
                && dimension == fingerprint.dimension
                && policyRevision == fingerprint.policyRevision
                && trustedRevision == fingerprint.trustedRevision
                && persistentWorldKey.equals(fingerprint.persistentWorldKey)
                && trustedCapacityState.equals(fingerprint.trustedCapacityState)
                && semanticEntries.equals(fingerprint.semanticEntries);
    }

    @Override
    public int hashCode() {
        int result = System.identityHashCode(worldIdentity);
        result = 31 * result + System.identityHashCode(userTaskRoot);
        result = 31 * result + dimension.hashCode();
        result = 31 * result + persistentWorldKey.hashCode();
        result = 31 * result + policyRevision;
        result = 31 * result + Long.hashCode(trustedRevision);
        result = 31 * result + trustedCapacityState.hashCode();
        result = 31 * result + semanticEntries.hashCode();
        return result;
    }
}
