package lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added an invariant-checked result for side-effect-free anchor reads.
public final class AutoDepositBulkAnchorReadResult {
    private final AutoDepositBulkAnchorReadStatus status;
    private final AutoDepositBulkPlayerAnchor anchor;

    private AutoDepositBulkAnchorReadResult(
            AutoDepositBulkAnchorReadStatus status,
            AutoDepositBulkPlayerAnchor anchor) {
        this.status = Objects.requireNonNull(status, "status");
        if ((status == AutoDepositBulkAnchorReadStatus.AVAILABLE) != (anchor != null)) {
            throw new IllegalArgumentException(
                    "AVAILABLE requires an anchor and non-available outcomes forbid one"
            );
        }
        this.anchor = anchor;
    }

    public static AutoDepositBulkAnchorReadResult available(
            AutoDepositBulkPlayerAnchor anchor) {
        return new AutoDepositBulkAnchorReadResult(
                AutoDepositBulkAnchorReadStatus.AVAILABLE,
                Objects.requireNonNull(anchor, "anchor")
        );
    }

    public static AutoDepositBulkAnchorReadResult unavailable() {
        return new AutoDepositBulkAnchorReadResult(
                AutoDepositBulkAnchorReadStatus.UNAVAILABLE,
                null
        );
    }

    public static AutoDepositBulkAnchorReadResult playerWorldMembershipMismatch() {
        return new AutoDepositBulkAnchorReadResult(
                AutoDepositBulkAnchorReadStatus.PLAYER_WORLD_MEMBERSHIP_MISMATCH,
                null
        );
    }

    public AutoDepositBulkAnchorReadStatus status() {
        return status;
    }

    public Optional<AutoDepositBulkPlayerAnchor> anchor() {
        return Optional.ofNullable(anchor);
    }
}
