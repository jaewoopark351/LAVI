package lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkLogicalDestination {
    private final AutoDepositBulkContainerKind kind;
    private final List<BlockPos> memberPositions;
    private final AutoDepositDoubleChestPairKey pairKey;

    private AutoDepositBulkLogicalDestination(
            AutoDepositBulkContainerKind kind,
            List<BlockPos> memberPositions,
            AutoDepositDoubleChestPairKey pairKey) {
        this.kind = Objects.requireNonNull(kind, "kind");
        if (!kind.supported()) {
            throw new IllegalArgumentException("logical destination kind must be supported");
        }
        this.memberPositions = Objects.requireNonNull(memberPositions, "memberPositions")
                .stream()
                .map(position -> Objects.requireNonNull(position, "member position").toImmutable())
                .toList();
        this.pairKey = pairKey;
        int expectedSize = pairKey == null ? 1 : 2;
        if (this.memberPositions.size() != expectedSize) {
            throw new IllegalArgumentException(
                    "logical destination member count does not match its topology"
            );
        }
        if (pairKey != null) {
            if (!kind.chest() || !this.memberPositions.equals(pairKey.positions())) {
                throw new IllegalArgumentException(
                        "double-chest destination requires its canonical pair members"
                );
            }
        }
    }

    public static AutoDepositBulkLogicalDestination single(
            AutoDepositBulkContainerKind kind,
            BlockPos position) {
        return new AutoDepositBulkLogicalDestination(
                kind,
                List.of(Objects.requireNonNull(position, "position").toImmutable()),
                null
        );
    }

    public static AutoDepositBulkLogicalDestination doubleChest(
            AutoDepositBulkContainerKind kind,
            AutoDepositDoubleChestPairKey pairKey) {
        AutoDepositDoubleChestPairKey checkedPair = Objects.requireNonNull(
                pairKey,
                "pairKey"
        );
        return new AutoDepositBulkLogicalDestination(
                kind,
                checkedPair.positions(),
                checkedPair
        );
    }

    public AutoDepositBulkContainerKind kind() {
        return kind;
    }

    public List<BlockPos> memberPositions() {
        return memberPositions;
    }

    public BlockPos canonicalFirst() {
        return memberPositions.get(0);
    }

    public boolean doubleChest() {
        return pairKey != null;
    }

    public Optional<AutoDepositDoubleChestPairKey> pairKey() {
        return Optional.ofNullable(pairKey);
    }
}
