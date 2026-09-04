package lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkLogicalDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositDoubleChestPairKey;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
class AutoDepositTrustedBulkMergePlannerTest {
    private static final String WORLD = "singleplayer:test";
    private final AutoDepositTrustedBulkMergePlanner planner =
            new AutoDepositTrustedBulkMergePlanner();

    @Test
    void preservesExistingOrderReenablesInPlaceAndAppendsNewPositionsDeterministically() {
        AutoDepositTrustedDestination unrelated = destination(new BlockPos(100, 64, 100), true);
        AutoDepositTrustedDestination disabled = destination(new BlockPos(4, 64, 4), false);
        AutoDepositTrustedDestination already = destination(new BlockPos(5, 64, 5), true);
        String disabledId = disabled.destinationId();

        AutoDepositTrustedBulkMergePlan plan = planner.plan(
                List.of(unrelated, disabled, already),
                WORLD,
                Dimension.OVERWORLD,
                List.of(
                        single(new BlockPos(9, 64, 9)),
                        single(disabled.position()),
                        single(new BlockPos(-2, 64, 3)),
                        single(already.position())
                )
        );

        assertEquals(AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED, plan.status());
        assertEquals(2, plan.newlyRegisteredCount());
        assertEquals(1, plan.reenabledCount());
        assertEquals(1, plan.alreadyRegisteredCount());
        assertEquals(List.of(
                unrelated.position(),
                disabled.position(),
                already.position(),
                new BlockPos(-2, 64, 3),
                new BlockPos(9, 64, 9)
        ), plan.finalDestinations().stream().map(AutoDepositTrustedDestination::position).toList());
        assertTrue(plan.finalDestinations().get(1).enabled());
        assertEquals(disabledId, plan.finalDestinations().get(1).destinationId());
    }

    @Test
    void newDoubleChestUsesCanonicalFirstRepresentative() {
        BlockPos first = new BlockPos(2, 64, 3);
        BlockPos second = new BlockPos(1, 64, 3);

        AutoDepositTrustedBulkMergePlan plan = planner.plan(
                List.of(),
                WORLD,
                Dimension.OVERWORLD,
                List.of(AutoDepositBulkLogicalDestination.doubleChest(
                        AutoDepositBulkContainerKind.CHEST,
                        AutoDepositDoubleChestPairKey.of(first, second)
                ))
        );

        assertEquals(AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED, plan.status());
        assertEquals(new BlockPos(1, 64, 3), plan.finalDestinations().get(0).position());
    }

    @Test
    void existingDoubleChestHalfPreservesItsPositionAndStableId() {
        BlockPos canonicalFirst = new BlockPos(1, 64, 3);
        BlockPos existingPosition = new BlockPos(2, 64, 3);
        AutoDepositTrustedDestination existing = destination(existingPosition, false);

        AutoDepositTrustedBulkMergePlan plan = planner.plan(
                List.of(existing),
                WORLD,
                Dimension.OVERWORLD,
                List.of(AutoDepositBulkLogicalDestination.doubleChest(
                        AutoDepositBulkContainerKind.CHEST,
                        AutoDepositDoubleChestPairKey.of(canonicalFirst, existingPosition)
                ))
        );

        assertEquals(AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED, plan.status());
        assertEquals(1, plan.reenabledCount());
        assertEquals(existingPosition, plan.finalDestinations().get(0).position());
        assertEquals(existing.destinationId(), plan.finalDestinations().get(0).destinationId());
        assertTrue(plan.finalDestinations().get(0).enabled());
    }

    @Test
    void twoPreexistingDoubleChestHalvesRejectTheWholePlan() {
        BlockPos first = new BlockPos(1, 64, 3);
        BlockPos second = new BlockPos(2, 64, 3);
        List<AutoDepositTrustedDestination> existing = List.of(
                destination(first, true),
                destination(second, true)
        );

        AutoDepositTrustedBulkMergePlan plan = planner.plan(
                existing,
                WORLD,
                Dimension.OVERWORLD,
                List.of(AutoDepositBulkLogicalDestination.doubleChest(
                        AutoDepositBulkContainerKind.CHEST,
                        AutoDepositDoubleChestPairKey.of(first, second)
                ))
        );

        assertEquals(
                AutoDepositTrustedBulkMergeStatus.PREEXISTING_DOUBLE_CHEST_DUPLICATE,
                plan.status()
        );
        assertEquals(existing, plan.finalDestinations());
        assertTrue(plan.firstConflict().length() <= 256);
    }

    @Test
    void plannerDoesNotTruncateMoreThanSixtyFourDestinations() {
        List<AutoDepositBulkLogicalDestination> logical = new ArrayList<>();
        for (int index = 0; index < 70; index++) {
            logical.add(single(new BlockPos(index, 64, 0)));
        }

        AutoDepositTrustedBulkMergePlan plan = planner.plan(
                List.of(),
                WORLD,
                Dimension.OVERWORLD,
                logical
        );

        assertEquals(AutoDepositTrustedBulkMergeStatus.MUTATION_REQUIRED, plan.status());
        assertEquals(70, plan.newlyRegisteredCount());
        assertEquals(70, plan.finalDestinations().size());
    }

    private static AutoDepositBulkLogicalDestination single(BlockPos position) {
        return AutoDepositBulkLogicalDestination.single(
                AutoDepositBulkContainerKind.BARREL,
                position
        );
    }

    private static AutoDepositTrustedDestination destination(BlockPos position, boolean enabled) {
        return new AutoDepositTrustedDestination(
                WORLD,
                Dimension.OVERWORLD,
                position,
                enabled
        );
    }
}
