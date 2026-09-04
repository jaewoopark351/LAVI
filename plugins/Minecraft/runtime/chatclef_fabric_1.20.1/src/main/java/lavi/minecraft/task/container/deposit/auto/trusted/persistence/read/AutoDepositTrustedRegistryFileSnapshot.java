package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import java.util.Arrays;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistryFileSnapshot {
    private final byte[] payload;
    private final AutoDepositTrustedRegistryProvenance provenance;

    public AutoDepositTrustedRegistryFileSnapshot(
            byte[] payload,
            AutoDepositTrustedRegistryProvenance provenance) {
        this.payload = Arrays.copyOf(Objects.requireNonNull(payload, "payload"), payload.length);
        this.provenance = Objects.requireNonNull(provenance, "provenance");
        if (!provenance.exists() && payload.length != 0) {
            throw new IllegalArgumentException("missing registry cannot have a payload");
        }
    }

    public byte[] payload() {
        return Arrays.copyOf(payload, payload.length);
    }

    public AutoDepositTrustedRegistryProvenance provenance() {
        return provenance;
    }
}
