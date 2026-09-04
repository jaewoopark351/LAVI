package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public record AutoDepositBulkBlockObservation(
        BlockPos position,
        AutoDepositBulkContainerKind kind,
        AutoDepositBulkChestPart chestPart,
        Optional<Direction> facing,
        Optional<BlockPos> partnerPosition,
        boolean discoveredInVolume) {
    public AutoDepositBulkBlockObservation {
        position = Objects.requireNonNull(position, "position").toImmutable();
        kind = Objects.requireNonNull(kind, "kind");
        chestPart = Objects.requireNonNull(chestPart, "chestPart");
        facing = Objects.requireNonNull(facing, "facing");
        partnerPosition = Objects.requireNonNull(partnerPosition, "partnerPosition")
                .map(BlockPos::toImmutable);

        if (kind.chest()) {
            if (chestPart == AutoDepositBulkChestPart.NONE || facing.isEmpty()) {
                throw new IllegalArgumentException("chest observation requires part and facing");
            }
            if (chestPart.doubleHalf() != partnerPosition.isPresent()) {
                throw new IllegalArgumentException(
                        "only a double-chest half may have a partner position"
                );
            }
        } else if (chestPart != AutoDepositBulkChestPart.NONE
                || facing.isPresent()
                || partnerPosition.isPresent()) {
            throw new IllegalArgumentException(
                    "non-chest observation must not contain chest topology"
            );
        }
    }

    public static AutoDepositBulkBlockObservation nonChest(
            BlockPos position,
            AutoDepositBulkContainerKind kind,
            boolean discoveredInVolume) {
        if (Objects.requireNonNull(kind, "kind").chest()) {
            throw new IllegalArgumentException("chest kind requires chest topology");
        }
        return new AutoDepositBulkBlockObservation(
                position,
                kind,
                AutoDepositBulkChestPart.NONE,
                Optional.empty(),
                Optional.empty(),
                discoveredInVolume
        );
    }

    public static AutoDepositBulkBlockObservation chest(
            BlockPos position,
            AutoDepositBulkContainerKind kind,
            AutoDepositBulkChestPart chestPart,
            Direction facing,
            BlockPos partnerPosition,
            boolean discoveredInVolume) {
        return new AutoDepositBulkBlockObservation(
                position,
                kind,
                chestPart,
                Optional.of(Objects.requireNonNull(facing, "facing")),
                Optional.ofNullable(partnerPosition),
                discoveredInVolume
        );
    }

    public AutoDepositBulkBlockObservation withDiscoveredInVolume(boolean discovered) {
        if (discovered == discoveredInVolume) {
            return this;
        }
        return new AutoDepositBulkBlockObservation(
                position,
                kind,
                chestPart,
                facing,
                partnerPosition,
                discovered
        );
    }
}
