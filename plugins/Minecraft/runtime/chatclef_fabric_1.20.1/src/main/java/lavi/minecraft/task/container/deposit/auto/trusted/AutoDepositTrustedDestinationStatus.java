package lavi.minecraft.task.container.deposit.auto.trusted;

//20260827_kpopmodder: Present conservative trusted destination health without claiming live proof.
public enum AutoDepositTrustedDestinationStatus {
    KNOWN_AVAILABLE,
    KNOWN_FULL,
    MISSING,
    KNOWN_UNREACHABLE,
    UNKNOWN_OR_STALE
}
