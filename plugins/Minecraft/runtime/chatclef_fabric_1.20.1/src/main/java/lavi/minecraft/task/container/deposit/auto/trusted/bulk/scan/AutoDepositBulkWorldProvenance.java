package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import adris.altoclef.util.Dimension;

import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkWorldProvenance {
    private final String worldKey;
    private final Dimension dimension;
    private final String dimensionKey;
    private final Object worldIdentity;

    public AutoDepositBulkWorldProvenance(
            String worldKey,
            Dimension dimension,
            String dimensionKey,
            Object worldIdentity) {
        this.worldKey = requireText(worldKey, "worldKey");
        this.dimension = Objects.requireNonNull(dimension, "dimension");
        this.dimensionKey = requireText(dimensionKey, "dimensionKey");
        this.worldIdentity = Objects.requireNonNull(worldIdentity, "worldIdentity");
    }

    public String worldKey() {
        return worldKey;
    }

    public Dimension dimension() {
        return dimension;
    }

    public String dimensionKey() {
        return dimensionKey;
    }

    public boolean sameWorld(AutoDepositBulkWorldProvenance other) {
        return other != null
                && worldIdentity == other.worldIdentity
                && worldKey.equals(other.worldKey)
                && dimension == other.dimension
                && dimensionKey.equals(other.dimensionKey);
    }

    public boolean matchesWorldIdentity(Object candidate) {
        return candidate != null && worldIdentity == candidate;
    }

    private static String requireText(String value, String name) {
        String normalized = Objects.requireNonNull(value, name).trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
