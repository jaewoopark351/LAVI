package lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBlockObservation;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositBulkTopologyNormalizer {
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    public AutoDepositBulkTopologyResult normalize(AutoDepositBulkScanResult scanResult) {
        AutoDepositBulkScanResult scan = Objects.requireNonNull(scanResult, "scanResult");
        if (!scan.success()) {
            throw new IllegalArgumentException("topology requires a complete scan result");
        }

        Map<BlockPos, AutoDepositBulkBlockObservation> observations = new HashMap<>();
        for (AutoDepositBulkBlockObservation observation : scan.observations()) {
            observations.put(observation.position(), observation);
        }
        List<AutoDepositBulkBlockObservation> discovered = scan.observations().stream()
                .filter(AutoDepositBulkBlockObservation::discoveredInVolume)
                .filter(observation -> observation.kind().supported())
                .sorted(Comparator.comparing(
                        AutoDepositBulkBlockObservation::position,
                        POSITION_ORDER
                ))
                .toList();

        List<AutoDepositBulkLogicalDestination> logicalDestinations = new ArrayList<>();
        Set<BlockPos> consumed = new HashSet<>();
        for (AutoDepositBulkBlockObservation observation : discovered) {
            if (consumed.contains(observation.position())) {
                continue;
            }
            if (!observation.kind().chest()
                    || !observation.chestPart().doubleHalf()) {
                logicalDestinations.add(AutoDepositBulkLogicalDestination.single(
                        observation.kind(),
                        observation.position()
                ));
                consumed.add(observation.position());
                continue;
            }

            BlockPos partnerPosition = observation.partnerPosition().orElseThrow();
            AutoDepositBulkBlockObservation partner = observations.get(partnerPosition);
            String invalidReason = invalidPairReason(observation, partner);
            if (invalidReason != null) {
                return AutoDepositBulkTopologyResult.ambiguous(
                        invalidReason,
                        "pos=" + observation.position().toShortString()
                                + ",partner=" + partnerPosition.toShortString()
                );
            }

            AutoDepositDoubleChestPairKey pairKey;
            try {
                pairKey = AutoDepositDoubleChestPairKey.of(
                        observation.position(),
                        partnerPosition
                );
            } catch (IllegalArgumentException exception) {
                return AutoDepositBulkTopologyResult.ambiguous(
                        "double_chest_partner_not_adjacent",
                        "pos=" + observation.position().toShortString()
                                + ",partner=" + partnerPosition.toShortString()
                );
            }
            logicalDestinations.add(AutoDepositBulkLogicalDestination.doubleChest(
                    observation.kind(),
                    pairKey
            ));
            consumed.add(observation.position());
            consumed.add(partnerPosition);
        }

        int collapsedHalfCount = Math.max(
                0,
                scan.physicalSupportedBlockCount() - logicalDestinations.size()
        );
        return AutoDepositBulkTopologyResult.success(
                logicalDestinations,
                collapsedHalfCount
        );
    }

    private static String invalidPairReason(
            AutoDepositBulkBlockObservation first,
            AutoDepositBulkBlockObservation second) {
        if (second == null) {
            return "double_chest_partner_missing";
        }
        if (first.kind() != second.kind()) {
            return "double_chest_kind_mismatch";
        }
        if (!second.kind().chest()) {
            return "double_chest_partner_not_chest";
        }
        if (!first.chestPart().complements(second.chestPart())) {
            return "double_chest_parts_not_complementary";
        }
        if (first.facing().isEmpty()
                || second.facing().isEmpty()
                || first.facing().get() != second.facing().get()) {
            return "double_chest_facing_mismatch";
        }
        if (!pointsToPartner(first, second.position())
                || !pointsToPartner(second, first.position())) {
            return "double_chest_partner_direction_mismatch";
        }
        if (second.partnerPosition().isEmpty()
                || !second.partnerPosition().get().equals(first.position())) {
            return "double_chest_partner_not_reciprocal";
        }
        return null;
    }

    private static boolean pointsToPartner(
            AutoDepositBulkBlockObservation observation,
            BlockPos partnerPosition) {
        Direction facing = observation.facing().orElseThrow();
        Direction partnerDirection = switch (observation.chestPart()) {
            case LEFT -> facing.rotateYClockwise();
            case RIGHT -> facing.rotateYCounterclockwise();
            default -> throw new IllegalArgumentException(
                    "double-chest validation requires LEFT or RIGHT"
            );
        };
        return observation.position().offset(partnerDirection).equals(partnerPosition);
    }
}
