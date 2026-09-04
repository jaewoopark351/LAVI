package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkLogicalDestination;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkMergePlanner {
    private static final int MAX_CONFLICT_LENGTH = 256;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
            .comparingInt(BlockPos::getX)
            .thenComparingInt(BlockPos::getY)
            .thenComparingInt(BlockPos::getZ);

    public AutoDepositTrustedBulkMergePlan plan(
            List<AutoDepositTrustedDestination> existingDestinations,
            String worldKey,
            Dimension dimension,
            List<AutoDepositBulkLogicalDestination> logicalDestinations) {
        if (existingDestinations == null
                || worldKey == null
                || worldKey.isBlank()
                || !worldKey.equals(worldKey.trim())
                || dimension == null
                || logicalDestinations == null) {
            return invalid(existingDestinations, "invalid_bulk_registration_request");
        }

        List<AutoDepositTrustedDestination> updated = new ArrayList<>(existingDestinations);
        Map<String, Integer> existingIndexByKey = new HashMap<>();
        for (int index = 0; index < updated.size(); index++) {
            AutoDepositTrustedDestination destination = updated.get(index);
            if (destination == null || existingIndexByKey.put(destination.key(), index) != null) {
                return invalid(existingDestinations, "invalid_or_duplicate_existing_destination");
            }
        }

        Set<String> claimedPhysicalKeys = new HashSet<>();
        List<BlockPos> newRepresentatives = new ArrayList<>();
        int reenabledCount = 0;
        int alreadyRegisteredCount = 0;

        for (AutoDepositBulkLogicalDestination logical : logicalDestinations) {
            if (logical == null) {
                return invalid(existingDestinations, "null_logical_destination");
            }
            List<BlockPos> members = logical.memberPositions();
            int expectedSize = logical.doubleChest() ? 2 : 1;
            if (members == null || members.size() != expectedSize) {
                return invalid(existingDestinations, "invalid_logical_destination_members");
            }

            List<Integer> existingIndexes = new ArrayList<>();
            Set<String> logicalKeys = new HashSet<>();
            for (BlockPos member : members) {
                if (member == null) {
                    return invalid(existingDestinations, "null_logical_destination_member");
                }
                String key = destinationKey(worldKey, dimension, member);
                if (!logicalKeys.add(key) || !claimedPhysicalKeys.add(key)) {
                    return invalid(existingDestinations, "duplicate_logical_destination_member:" + position(member));
                }
                Integer existingIndex = existingIndexByKey.get(key);
                if (existingIndex != null) {
                    existingIndexes.add(existingIndex);
                }
            }

            if (logical.doubleChest() && existingIndexes.size() == 2) {
                return AutoDepositTrustedBulkMergePlan.of(
                        AutoDepositTrustedBulkMergeStatus.PREEXISTING_DOUBLE_CHEST_DUPLICATE,
                        existingDestinations,
                        0,
                        0,
                        0,
                        bounded("both_double_chest_halves_registered:" + positions(members))
                );
            }
            if (existingIndexes.size() > 1) {
                return invalid(existingDestinations, "multiple_existing_representatives:" + positions(members));
            }

            if (existingIndexes.size() == 1) {
                int existingIndex = existingIndexes.get(0);
                AutoDepositTrustedDestination existing = updated.get(existingIndex);
                if (existing.enabled()) {
                    alreadyRegisteredCount++;
                } else {
                    updated.set(existingIndex, new AutoDepositTrustedDestination(
                            existing.worldKey(),
                            existing.dimension(),
                            existing.position(),
                            true
                    ));
                    reenabledCount++;
                }
                continue;
            }

            BlockPos representative = logical.canonicalFirst();
            if (representative == null || !members.contains(representative)) {
                return invalid(existingDestinations, "invalid_canonical_representative");
            }
            newRepresentatives.add(representative.toImmutable());
        }

        newRepresentatives.sort(POSITION_ORDER);
        for (BlockPos representative : newRepresentatives) {
            updated.add(new AutoDepositTrustedDestination(
                    worldKey,
                    dimension,
                    representative,
                    true
            ));
        }
        AutoDepositTrustedBulkMergeStatus status = newRepresentatives.isEmpty() && reenabledCount == 0
                ? AutoDepositTrustedBulkMergeStatus.NO_CHANGE
                : AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED;
        return AutoDepositTrustedBulkMergePlan.of(
                status,
                updated,
                newRepresentatives.size(),
                reenabledCount,
                alreadyRegisteredCount,
                ""
        );
    }

    private static AutoDepositTrustedBulkMergePlan invalid(
            List<AutoDepositTrustedDestination> existing,
            String conflict) {
        return AutoDepositTrustedBulkMergePlan.of(
                AutoDepositTrustedBulkMergeStatus.INVALID_REQUEST,
                existing == null ? List.of() : existing,
                0,
                0,
                0,
                bounded(conflict)
        );
    }

    private static String destinationKey(String worldKey, Dimension dimension, BlockPos position) {
        return worldKey + "|" + dimension.name()
                + "|" + position.getX()
                + "|" + position.getY()
                + "|" + position.getZ();
    }

    private static String positions(List<BlockPos> positions) {
        return positions.stream().filter(Objects::nonNull)
                .map(AutoDepositTrustedBulkMergePlanner::position)
                .reduce((left, right) -> left + "," + right)
                .orElse("unavailable");
    }

    private static String position(BlockPos position) {
        return position.getX() + ":" + position.getY() + ":" + position.getZ();
    }

    private static String bounded(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_CONFLICT_LENGTH
                ? value
                : value.substring(0, MAX_CONFLICT_LENGTH);
    }
}
