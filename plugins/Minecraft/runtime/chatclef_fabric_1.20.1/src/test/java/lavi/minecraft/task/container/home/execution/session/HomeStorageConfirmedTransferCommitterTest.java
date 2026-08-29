package lavi.minecraft.task.container.home.execution.session;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.operation.HomeStorageTransferIdentity;
import lavi.minecraft.task.container.home.planning.HomeStorageDisposition;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshot;
import lavi.minecraft.task.container.home.planning.HomeStorageManifest;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.planning.HomeStoragePlan;
import lavi.minecraft.task.container.home.planning.HomeStoragePlanEntry;
import lavi.minecraft.task.container.home.planning.HomeStorageStackFingerprint;
import lavi.minecraft.task.container.home.planning.HomeStorageStackLocation;
import lavi.minecraft.task.container.home.planning.HomeStorageStackSnapshot;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260828_kpopmodder: Verify no-yield composite confirmation and replay behavior.
class HomeStorageConfirmedTransferCommitterTest {
    @Test
    void preparesBothSessionAndOperationSidesExactlyOnce() {
        Fixture fixture = fixture();
        HomeStorageConfirmedTransferCommitter committer =
                new HomeStorageConfirmedTransferCommitter();

        HomeStorageConfirmedTransferCommit first = committer.prepare(
                fixture.operation(), fixture.pendingSession(), fixture.step(), 12, 0
        );
        HomeStorageConfirmedTransferCommit replay = committer.prepare(
                first.operation(), fixture.pendingSession(), fixture.step(), 12, 0
        );

        assertTrue(first.newlyCommitted());
        assertEquals(12, first.operation().storedItems());
        assertEquals(0, first.operation().latestRemainingStacks());
        assertTrue(first.session().progress().complete());
        assertTrue(first.session().pendingTransfer().isEmpty());
        assertFalse(replay.newlyCommitted());
        assertSame(first.operation(), replay.operation());
        assertEquals(12, replay.operation().storedItems());
    }

    @Test
    void conflictingReplayLeavesImmutableInputsUnchanged() {
        Fixture fixture = fixture();
        HomeStorageConfirmedTransferCommitter committer =
                new HomeStorageConfirmedTransferCommitter();
        HomeStorageConfirmedTransferCommit first = committer.prepare(
                fixture.operation(), fixture.pendingSession(), fixture.step(), 12, 0
        );

        assertThrows(IllegalStateException.class, () -> committer.prepare(
                first.operation(), fixture.pendingSession(), fixture.step(), 5, 7
        ));
        assertEquals(12, first.operation().storedItems());
        assertEquals(12, fixture.pendingSession().progress()
                .expectedCount(fixture.step()));
        assertTrue(fixture.pendingSession().pendingTransfer().isPresent());
    }

    @Test
    void pendingIdentityRejectsAnotherOperationOrHandlerBinding() {
        Fixture fixture = fixture();
        HomeStorageTransferIdentity identity = fixture.pendingSession()
                .pendingTransfer().orElseThrow();

        assertTrue(fixture.pendingSession().ownsPendingTransfer(identity, 17L));
        assertFalse(fixture.pendingSession().ownsPendingTransfer(identity, 18L));
        assertFalse(fixture.pendingSession().ownsPendingTransfer(
                new HomeStorageTransferIdentity(
                        identity.operationId(),
                        identity.containerSessionOrdinal(),
                        identity.planRevision(),
                        identity.attemptOrdinal(),
                        identity.logicalPlayerSlot(),
                        identity.destinationKey(),
                        new Object(),
                        identity.handlerSyncId()
                ),
                17L
        ));
    }

    private static Fixture fixture() {
        HomeStorageStackFingerprint fingerprint =
                HomeStorageStackFingerprint.of("minecraft:redstone", 0, null);
        HomeStorageStackSnapshot stack = new HomeStorageStackSnapshot(
                0,
                HomeStorageStackLocation.MAIN,
                fingerprint,
                12,
                AutoDepositItemRole.NONE,
                0,
                0,
                0,
                0,
                true,
                false,
                false,
                0
        );
        HomeStorageInventorySnapshot inventory = new HomeStorageInventorySnapshot(
                List.of(HomeStorageInventorySlotSnapshot.occupied(
                        HomeStorageStackLocation.MAIN, 0, fingerprint, 12
                )),
                List.of(stack),
                0
        );
        HomeStorageManifestStep step = new HomeStorageManifestStep(
                0,
                fingerprint,
                12,
                HomeStorageManifestStep.TransferMode.WHOLE_STACK_QUICK_MOVE,
                HomeStorageDisposition.STORE_HOME,
                "manual_home_surplus",
                3L
        );
        HomeStoragePlan plan = new HomeStoragePlan(
                3L,
                List.of(new HomeStoragePlanEntry(
                        stack,
                        HomeStorageDisposition.STORE_HOME,
                        "manual_home_surplus"
                )),
                new HomeStorageManifest(3L, List.of(step))
        );
        HomeStorageActivationBaseline baseline =
                new HomeStorageActivationBaseline(inventory, plan);
        AutoDepositTrustedDestination destination =
                new AutoDepositTrustedDestination(
                        "test-world",
                        Dimension.OVERWORLD,
                        new BlockPos(1, 64, 1),
                        true
                );
        AutoDepositTrustedDestinationCandidate candidate =
                new AutoDepositTrustedDestinationCandidate(
                        destination, 0, 0.0, "test"
                );
        HomeStorageContainerSession pending = HomeStorageContainerSession.activate(
                candidate,
                1,
                baseline,
                HomeStorageContainerActivation.ready(new Object(), 4)
        ).beginTransfer(17L, step);
        StoreHomeOperationProgress operation = StoreHomeOperationProgress.start(17L)
                .activatedSession(1);
        return new Fixture(operation, pending, step);
    }

    private record Fixture(
            StoreHomeOperationProgress operation,
            HomeStorageContainerSession pendingSession,
            HomeStorageManifestStep step) {
    }
}
