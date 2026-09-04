package lavi.minecraft.task.container.deposit.auto.trusted.bulk;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkPlayerAnchor;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;

import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkRegistrationSnapshot {
    private final AutoDepositBulkAnchorKind anchorKind;
    private final AutoDepositBulkPlayerAnchor playerAnchor;
    private final AutoDepositBulkWorldProvenance worldProvenance;

    public AutoDepositBulkRegistrationSnapshot(
            AutoDepositBulkAnchorKind anchorKind,
            AutoDepositBulkPlayerAnchor playerAnchor,
            AutoDepositBulkWorldProvenance worldProvenance) {
        this.anchorKind = Objects.requireNonNull(anchorKind, "anchorKind");
        this.playerAnchor = Objects.requireNonNull(playerAnchor, "playerAnchor");
        this.worldProvenance = Objects.requireNonNull(worldProvenance, "worldProvenance");
        if (!playerAnchor.belongsToWorld(worldProvenance)) {
            throw new IllegalArgumentException(
                    "player anchor and world provenance must share one world identity"
            );
        }
    }

    public AutoDepositBulkAnchorKind anchorKind() {
        return anchorKind;
    }

    public AutoDepositBulkPlayerAnchor playerAnchor() {
        return playerAnchor;
    }

    public AutoDepositBulkWorldProvenance worldProvenance() {
        return worldProvenance;
    }

    public boolean sameWorld(AutoDepositBulkWorldProvenance candidate) {
        return worldProvenance.sameWorld(candidate);
    }
}
