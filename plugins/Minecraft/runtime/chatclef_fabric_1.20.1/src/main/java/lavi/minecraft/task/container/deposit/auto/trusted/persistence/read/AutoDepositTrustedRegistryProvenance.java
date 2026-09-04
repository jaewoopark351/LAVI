package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistryProvenance {
    private final boolean exists;
    private final long size;
    private final long modifiedTimeMillis;
    private final String sha256;

    private AutoDepositTrustedRegistryProvenance(
            boolean exists,
            long size,
            long modifiedTimeMillis,
            String sha256) {
        this.exists = exists;
        this.size = size;
        this.modifiedTimeMillis = modifiedTimeMillis;
        this.sha256 = Objects.requireNonNull(sha256, "sha256");
    }

    public static AutoDepositTrustedRegistryProvenance missing() {
        return new AutoDepositTrustedRegistryProvenance(false, 0L, -1L, "");
    }

    public static AutoDepositTrustedRegistryProvenance present(
            long size,
            long modifiedTimeMillis,
            String sha256) {
        if (size < 0L || modifiedTimeMillis < 0L) {
            throw new IllegalArgumentException("present provenance requires non-negative size and mtime");
        }
        String normalizedSha256 = Objects.requireNonNull(sha256, "sha256").trim();
        if (normalizedSha256.isEmpty()) {
            throw new IllegalArgumentException("present provenance requires sha256");
        }
        return new AutoDepositTrustedRegistryProvenance(
                true,
                size,
                modifiedTimeMillis,
                normalizedSha256
        );
    }

    public boolean exists() {
        return exists;
    }

    public long size() {
        return size;
    }

    public long modifiedTimeMillis() {
        return modifiedTimeMillis;
    }

    public String sha256() {
        return sha256;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AutoDepositTrustedRegistryProvenance that)) {
            return false;
        }
        return exists == that.exists
                && size == that.size
                && modifiedTimeMillis == that.modifiedTimeMillis
                && sha256.equals(that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exists, size, modifiedTimeMillis, sha256);
    }
}
