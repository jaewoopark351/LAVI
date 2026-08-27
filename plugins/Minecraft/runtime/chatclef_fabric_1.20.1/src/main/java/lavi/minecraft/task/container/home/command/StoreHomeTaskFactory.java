package lavi.minecraft.task.container.home.command;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageDestinationSelector;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomeTask;
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
        HomeStorageScreenSlotResolver slotResolver = new HomeStorageScreenSlotResolver();
        return new StoreHomeTask(
                repository,
                exactOpenContainerBinding,
                new AutoDepositWorldKeyReader(),
                new HomeStorageInventorySnapshotReader(),
                new HomeLoadoutPlanner(),
                new HomeStorageDestinationSelector(repository),
                new HomeStorageTransferExecutor(slotResolver)
        );
    }
}
