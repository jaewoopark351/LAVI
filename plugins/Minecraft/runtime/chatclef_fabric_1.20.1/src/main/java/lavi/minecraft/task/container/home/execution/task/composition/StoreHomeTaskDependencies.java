package lavi.minecraft.task.container.home.execution.task.composition;

import lavi.minecraft.diagnostics.container.home.StoreHomeManifestStaleDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageDestinationSelector;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
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

//20260829_kpopmodder: Keep the public StoreHomeTask constructor contract as one validated dependency set.
public final class StoreHomeTaskDependencies {
    private final AutoDepositExactOpenContainerBinding exactOpenContainerBinding;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final HomeStorageInventorySnapshotReader snapshotReader;
    private final HomeLoadoutPlanner planner;
    private final HomeStorageDestinationSelector destinationSelector;
    private final HomeStorageScreenSlotResolver slotResolver;
    private final HomeStorageTransferExecutor transferExecutor;
    private final StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics;
    private final StoreHomeCandidateValidator candidateValidator;
    private final HomeStorageContainerActivationGate activationGate;
    private final HomeStorageManifestValidator manifestValidator;
    private final HomeStorageConfirmedTransferCommitter transferCommitter;
    private final StoreHomeTerminalClassifier terminalClassifier;
    private final StoreHomeTerminalReporter terminalReporter;
    private final StoreHomeOperationProgress operation;

    public StoreHomeTaskDependencies(
            AutoDepositExactOpenContainerBinding exactOpenContainerBinding,
            AutoDepositWorldKeyReader worldKeyReader,
            HomeStorageInventorySnapshotReader snapshotReader,
            HomeLoadoutPlanner planner,
            HomeStorageDestinationSelector destinationSelector,
            HomeStorageScreenSlotResolver slotResolver,
            HomeStorageTransferExecutor transferExecutor,
            StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics,
            StoreHomeCandidateValidator candidateValidator,
            HomeStorageContainerActivationGate activationGate,
            HomeStorageManifestValidator manifestValidator,
            HomeStorageConfirmedTransferCommitter transferCommitter,
            StoreHomeTerminalClassifier terminalClassifier,
            StoreHomeTerminalReporter terminalReporter,
            StoreHomeOperationProgress operation) {
        this.exactOpenContainerBinding = Objects.requireNonNull(
                exactOpenContainerBinding, "exactOpenContainerBinding"
        );
        this.worldKeyReader = Objects.requireNonNull(
                worldKeyReader, "worldKeyReader"
        );
        this.snapshotReader = Objects.requireNonNull(
                snapshotReader, "snapshotReader"
        );
        this.planner = Objects.requireNonNull(planner, "planner");
        this.destinationSelector = Objects.requireNonNull(
                destinationSelector, "destinationSelector"
        );
        this.slotResolver = Objects.requireNonNull(slotResolver, "slotResolver");
        this.transferExecutor = Objects.requireNonNull(
                transferExecutor, "transferExecutor"
        );
        this.manifestStaleDiagnostics = Objects.requireNonNull(
                manifestStaleDiagnostics, "manifestStaleDiagnostics"
        );
        this.candidateValidator = Objects.requireNonNull(
                candidateValidator, "candidateValidator"
        );
        this.activationGate = Objects.requireNonNull(
                activationGate, "activationGate"
        );
        this.manifestValidator = Objects.requireNonNull(
                manifestValidator, "manifestValidator"
        );
        this.transferCommitter = Objects.requireNonNull(
                transferCommitter, "transferCommitter"
        );
        this.terminalClassifier = Objects.requireNonNull(
                terminalClassifier, "terminalClassifier"
        );
        this.terminalReporter = Objects.requireNonNull(
                terminalReporter, "terminalReporter"
        );
        this.operation = Objects.requireNonNull(operation, "operation");
    }

    public AutoDepositExactOpenContainerBinding exactOpenContainerBinding() {
        return exactOpenContainerBinding;
    }

    public AutoDepositWorldKeyReader worldKeyReader() {
        return worldKeyReader;
    }

    public HomeStorageInventorySnapshotReader snapshotReader() {
        return snapshotReader;
    }

    public HomeLoadoutPlanner planner() {
        return planner;
    }

    public HomeStorageDestinationSelector destinationSelector() {
        return destinationSelector;
    }

    public HomeStorageScreenSlotResolver slotResolver() {
        return slotResolver;
    }

    public HomeStorageTransferExecutor transferExecutor() {
        return transferExecutor;
    }

    public StoreHomeManifestStaleDiagnostics manifestStaleDiagnostics() {
        return manifestStaleDiagnostics;
    }

    public StoreHomeCandidateValidator candidateValidator() {
        return candidateValidator;
    }

    public HomeStorageContainerActivationGate activationGate() {
        return activationGate;
    }

    public HomeStorageManifestValidator manifestValidator() {
        return manifestValidator;
    }

    public HomeStorageConfirmedTransferCommitter transferCommitter() {
        return transferCommitter;
    }

    public StoreHomeTerminalClassifier terminalClassifier() {
        return terminalClassifier;
    }

    public StoreHomeTerminalReporter terminalReporter() {
        return terminalReporter;
    }

    public StoreHomeOperationProgress operation() {
        return operation;
    }
}
