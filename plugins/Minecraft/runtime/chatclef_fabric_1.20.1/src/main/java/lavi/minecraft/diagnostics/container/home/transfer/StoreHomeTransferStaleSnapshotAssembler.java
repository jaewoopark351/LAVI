package lavi.minecraft.diagnostics.container.home.transfer;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.home.StoreHomeDestinationSnapshot;
import lavi.minecraft.diagnostics.container.home.StoreHomeHandlerSnapshot;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestFailureStage;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestMismatchSnapshot;
import lavi.minecraft.diagnostics.container.home.StoreHomeTransferStaleSnapshot;
import lavi.minecraft.diagnostics.container.home.slot.StoreHomeScreenSlotSnapshotReader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureObservation;
import lavi.minecraft.task.container.home.execution.transfer.failure.HomeStorageTransferFailureStage;
import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import net.minecraft.item.ItemStack;

import java.util.Objects;

//20260829_kpopmodder: Assemble stale-transfer diagnostic DTOs outside transfer behavior.
public final class StoreHomeTransferStaleSnapshotAssembler {
    private final StoreHomeScreenSlotSnapshotReader slotSnapshotReader;
    private final StoreHomePendingTransferSnapshotConverter pendingConverter =
            new StoreHomePendingTransferSnapshotConverter();

    public StoreHomeTransferStaleSnapshotAssembler(
            HomeStorageScreenSlotResolver slotResolver) {
        slotSnapshotReader = new StoreHomeScreenSlotSnapshotReader(
                Objects.requireNonNull(slotResolver, "slotResolver")
        );
    }

    public StoreHomeTransferStaleSnapshot assemble(
            AltoClef mod,
            AutoDepositTrustedDestination destination,
            AutoDepositExactOpenContainerBinding binding,
            HomeStorageManifestStep step,
            HomeStorageTransferFailureObservation observation) {
        Objects.requireNonNull(observation, "observation");
        ItemStack actual = observation.actualStack();
        boolean fingerprintMatched = observation.fingerprintMatched();
        if (observation.stage() == HomeStorageTransferFailureStage.SLOT_RESOLUTION) {
            actual = mod.getPlayer().getInventory().main.get(
                    step.logicalPlayerInventorySlot()
            );
            fingerprintMatched = actual != null
                    && !actual.isEmpty()
                    && step.fingerprint().matches(actual);
        }
        return new StoreHomeTransferStaleSnapshot(
                diagnosticStage(observation.stage()),
                StoreHomeManifestMismatchSnapshot.capture(
                        step,
                        observation.manifestStepIndex(),
                        observation.expectedSourceCount(),
                        actual,
                        fingerprintMatched,
                        observation.observationSource()
                ),
                slotSnapshotReader.inspect(
                        mod, step.logicalPlayerInventorySlot()
                ),
                pendingConverter.convert(observation.pendingTransfer()),
                StoreHomeHandlerSnapshot.capture(mod),
                StoreHomeDestinationSnapshot.capture(destination, binding)
        );
    }

    private static StoreHomeManifestFailureStage diagnosticStage(
            HomeStorageTransferFailureStage stage) {
        return switch (stage) {
            case SLOT_RESOLUTION ->
                    StoreHomeManifestFailureStage.EXECUTOR_SLOT_RESOLUTION;
            case PRE_CLICK_SOURCE_VALIDATION ->
                    StoreHomeManifestFailureStage.EXECUTOR_PRE_CLICK_SOURCE_VALIDATION;
            case POST_CLICK_SOURCE_VALIDATION ->
                    StoreHomeManifestFailureStage.EXECUTOR_POST_CLICK_SOURCE_VALIDATION;
        };
    }
}
