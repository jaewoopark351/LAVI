package lavi.minecraft.task.container.home.execution;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.transfer.HomeStoragePendingTransferObservation;
import lavi.minecraft.task.container.home.execution.transfer.HomeStorageTransferResult;
import lavi.minecraft.task.container.home.execution.transfer.HomeStorageTransferStatus;
import lavi.minecraft.task.container.home.execution.transfer.click.HomeStorageQuickMoveIssuer;
import lavi.minecraft.task.container.home.execution.transfer.click.HomeStorageQuickMoveOutcome;
import lavi.minecraft.task.container.home.execution.transfer.click.HomeStorageQuickMoveReadiness;
import lavi.minecraft.task.container.home.execution.transfer.click.HomeStorageQuickMoveReadinessChecker;
import lavi.minecraft.task.container.home.execution.transfer.container.HomeStorageContainerInventoryCalculator;
import lavi.minecraft.task.container.home.execution.transfer.container.HomeStorageLiveContainerInspector;
import lavi.minecraft.task.container.home.execution.transfer.container.HomeStorageLiveContainerSnapshot;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureObservation;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureStage;
import lavi.minecraft.task.container.home.execution.transfer.pending.HomeStoragePendingTransferState;
import lavi.minecraft.task.container.home.execution.transfer.pending.HomeStoragePendingTransferStatus;
import lavi.minecraft.task.container.home.execution.transfer.pending.HomeStoragePendingTransferTracker;
import lavi.minecraft.task.container.home.execution.transfer.pending.HomeStoragePendingTransferVerification;
import lavi.minecraft.task.container.home.execution.transfer.pending.HomeStoragePendingTransferVerifier;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

//20260829_kpopmodder: Orchestrate one exact transfer through focused behavior-owned collaborators.
public final class HomeStorageTransferExecutor {
    private static final int MAX_UNCONFIRMED_TICKS = 40;

    private final HomeStorageScreenSlotResolver slotResolver;
    private final HomeStorageLiveContainerInspector containerInspector =
            new HomeStorageLiveContainerInspector();
    private final HomeStorageContainerInventoryCalculator inventoryCalculator =
            new HomeStorageContainerInventoryCalculator();
    private final HomeStorageQuickMoveReadinessChecker readinessChecker =
            new HomeStorageQuickMoveReadinessChecker();
    private final HomeStorageQuickMoveIssuer quickMoveIssuer =
            new HomeStorageQuickMoveIssuer();
    private final HomeStoragePendingTransferTracker pendingTracker =
            new HomeStoragePendingTransferTracker();
    private final HomeStoragePendingTransferVerifier pendingVerifier =
            new HomeStoragePendingTransferVerifier();

    public HomeStorageTransferExecutor(HomeStorageScreenSlotResolver slotResolver) {
        this.slotResolver = Objects.requireNonNull(slotResolver, "slotResolver");
    }

    //20260829_kpopmodder: Preserve the original transfer entry point for existing callers.
    public HomeStorageTransferResult tick(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding,
            HomeStorageManifestStep step,
            int expectedSourceCount) {
        return tick(
                mod,
                destination,
                binding,
                step,
                expectedSourceCount,
                -1
        );
    }

    public HomeStorageTransferResult tick(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding,
            HomeStorageManifestStep step,
            int expectedSourceCount,
            int manifestStepIndex) {
        HomeStorageLiveContainerSnapshot live = containerInspector.inspect(
                mod, destination, binding
        );
        if (!live.open()) {
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.CONTAINER_NOT_OPEN,
                    live.reason()
            );
        }
        if (readinessChecker.cursor(live.handler())
                == HomeStorageQuickMoveReadiness.CURSOR_NOT_EMPTY) {
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.CURSOR_NOT_EMPTY,
                    "cursor_not_empty"
            );
        }

        OptionalInt resolved = slotResolver.resolve(
                mod, step.logicalPlayerInventorySlot()
        );
        if (resolved.isEmpty()) {
            return stale(
                    "logical_slot_mapping_unavailable",
                    HomeStorageTransferFailureStage.SLOT_RESOLUTION,
                    expectedSourceCount,
                    manifestStepIndex,
                    null,
                    false,
                    "player_main_inventory_after_mapping_failure"
            );
        }
        int sourceWindowSlot = resolved.getAsInt();
        ItemStack source = live.handler().slots.get(sourceWindowSlot).getStack();

        if (pendingTracker.hasPending()) {
            return verifyPending(
                    live,
                    destination,
                    step,
                    manifestStepIndex,
                    sourceWindowSlot,
                    source
            );
        }
        boolean fingerprintMatched = source != null
                && !source.isEmpty()
                && step.fingerprint().matches(source);
        if (source == null || source.isEmpty()
                || !fingerprintMatched
                || source.getCount() != expectedSourceCount) {
            return stale(
                    "exact_source_changed_before_click",
                    HomeStorageTransferFailureStage.PRE_CLICK_SOURCE_VALIDATION,
                    expectedSourceCount,
                    manifestStepIndex,
                    source,
                    fingerprintMatched,
                    "resolved_handler_source_slot"
            );
        }

        if (inventoryCalculator.availableCapacity(
                live, step.fingerprint(), source
        ) <= 0) {
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.NO_CAPACITY,
                    "trusted_gui_has_no_capacity"
            );
        }
        if (readinessChecker.slotAction(mod)
                == HomeStorageQuickMoveReadiness.SLOT_ACTION_DELAY) {
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.WAITING,
                    "slot_action_delay"
            );
        }

        pendingTracker.begin(
                destination.key(),
                step.logicalPlayerInventorySlot(),
                sourceWindowSlot,
                source.getCount(),
                inventoryCalculator.count(live, step.fingerprint())
        );
        HomeStorageQuickMoveOutcome issued = quickMoveIssuer.issue(
                mod, live.handler(), sourceWindowSlot
        );
        if (!issued.issued()) {
            pendingTracker.clear();
            mod.logWarning("Store-home exact slot click failed: "
                    + issued.exceptionClass());
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.TRANSFER_UNCONFIRMED,
                    issued.reason()
            );
        }
        return HomeStorageTransferResult.of(
                HomeStorageTransferStatus.CLICK_REQUESTED,
                issued.reason()
        );
    }

    public OptionalInt pendingLogicalSlot() {
        return pendingTracker.logicalSlot();
    }

    public boolean hasPending() {
        return pendingTracker.hasPending();
    }

    public void clearPending() {
        pendingTracker.clear();
    }

    public Optional<HomeStoragePendingTransferObservation> pendingObservation() {
        return pendingTracker.observation();
    }

    private HomeStorageTransferResult verifyPending(
            HomeStorageLiveContainerSnapshot live,
            AutoDepositTrustedDestination destination,
            HomeStorageManifestStep step,
            int manifestStepIndex,
            int sourceWindowSlot,
            ItemStack source) {
        HomeStoragePendingTransferState pending = pendingTracker.current()
                .orElseThrow(() -> new IllegalStateException(
                        "pending transfer disappeared during verification"
                ));
        HomeStoragePendingTransferVerification verification = pendingVerifier.verify(
                pending,
                destination.key(),
                step.logicalPlayerInventorySlot(),
                sourceWindowSlot,
                source,
                step.fingerprint(),
                () -> inventoryCalculator.count(live, step.fingerprint()),
                MAX_UNCONFIRMED_TICKS
        );
        if (verification.status()
                == HomeStoragePendingTransferStatus.FINGERPRINT_CHANGED) {
            HomeStorageTransferResult result = stale(
                    verification.reason(),
                    HomeStorageTransferFailureStage.POST_CLICK_SOURCE_VALIDATION,
                    pending.sourceCountBefore(),
                    manifestStepIndex,
                    source,
                    false,
                    "resolved_handler_source_slot_after_click"
            );
            pendingTracker.clear();
            return result;
        }
        if (verification.status() == HomeStoragePendingTransferStatus.CONFIRMED) {
            HomeStorageTransferResult result = new HomeStorageTransferResult(
                    HomeStorageTransferStatus.TRANSFERRED,
                    verification.transferredCount(),
                    verification.sourceCountAfter(),
                    verification.reason(),
                    null
            );
            pendingTracker.clear();
            return result;
        }
        if (verification.status() == HomeStoragePendingTransferStatus.WAITING) {
            pendingTracker.advanceTick();
            return HomeStorageTransferResult.of(
                    HomeStorageTransferStatus.WAITING,
                    verification.reason()
            );
        }
        if (verification.status() == HomeStoragePendingTransferStatus.TIMEOUT) {
            pendingTracker.advanceTick();
        }
        return terminal(
                HomeStorageTransferStatus.TRANSFER_UNCONFIRMED,
                verification.reason()
        );
    }

    private HomeStorageTransferResult stale(
            String reason,
            HomeStorageTransferFailureStage stage,
            int expectedSourceCount,
            int manifestStepIndex,
            ItemStack actual,
            boolean fingerprintMatched,
            String observationSource) {
        return HomeStorageTransferResult.stale(
                reason,
                new HomeStorageTransferFailureObservation(
                        stage,
                        expectedSourceCount,
                        manifestStepIndex,
                        actual,
                        fingerprintMatched,
                        observationSource,
                        pendingObservation()
                )
        );
    }

    private HomeStorageTransferResult terminal(
            HomeStorageTransferStatus status,
            String reason) {
        pendingTracker.clear();
        return HomeStorageTransferResult.of(status, reason);
    }
}
