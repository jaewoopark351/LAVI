package lavi.minecraft.task.container.home.command;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageDestinationSelector;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateValidator;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalClassifier;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeTerminalReporter;
import lavi.minecraft.task.container.home.execution.session.HomeStorageConfirmedTransferCommitter;
import lavi.minecraft.task.container.home.execution.session.HomeStorageContainerActivationGate;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidator;
import lavi.minecraft.task.container.home.planning.HomeLoadoutPlanner;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySnapshotReader;

import java.util.Objects;

//20260827_kpopmodder: Compose the canonical manual StoreHomeTask for every future input adapter.
public final class StoreHomeTaskFactory {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;

    public StoreHomeTaskFactory(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding, "exactOpenContainerBinding"
        );
    }

    public StoreHomeTask create() {
        long operationId = ChatClefDiagnostics.nextOperationId();
        AutoDepositWorldKeyReader worldKeyReader = new AutoDepositWorldKeyReader();
        HomeStorageScreenSlotResolver slotResolver = new HomeStorageScreenSlotResolver();
        StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics =
                new StoreHomeManifestStaleDiagnostics(operationId, slotResolver);
        return new StoreHomeTask(
                exactOpenContainerBinding,
                worldKeyReader,
                new HomeStorageInventorySnapshotReader(),
                new HomeLoadoutPlanner(),
                new HomeStorageDestinationSelector(repository),
                slotResolver,
                new HomeStorageTransferExecutor(slotResolver),
                manifestStaleDiagnostics,
                new StoreHomeCandidateValidator(repository),
                new HomeStorageContainerActivationGate(
                        repository,
                        exactOpenContainerBinding,
                        worldKeyReader
                ),
                new HomeStorageManifestValidator(),
                new HomeStorageConfirmedTransferCommitter(),
                new StoreHomeTerminalClassifier(),
                new StoreHomeTerminalReporter(),
                StoreHomeOperationProgress.start(operationId)
        );
    }
}
