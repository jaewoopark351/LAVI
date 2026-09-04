package lavi.minecraft.task.container.deposit.auto.trusted.command.single;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedCommandFormatter;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedTarget;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedTargetResolver;

import java.util.Objects;
import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
//20260904_kpopmodder: Preserve the existing exact-open-then-crosshair single registration behavior.
public final class AutoDepositExactTrustService implements AutoDepositSingleTrustOperation {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedTargetResolver targetResolver;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public AutoDepositExactTrustService(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedTargetResolver targetResolver,
            AutoDepositWorldKeyReader worldKeyReader) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    @Override
    public void register(AltoClef mod) {
        Optional<String> worldKey = worldKeyReader.read();
        Optional<AutoDepositTrustedTarget> target = targetResolver.resolve(mod);
        if (worldKey.isEmpty() || target.isEmpty() || mod.getWorld() == null) {
            mod.logWarning("Trusted destination not registered: look at a supported container or use an exactly bound open container.");
            return;
        }
        Dimension dimension = WorldHelper.getCurrentDimension();
        AutoDepositTrustedDestination destination = new AutoDepositTrustedDestination(
                worldKey.get(), dimension, target.get().position(), true
        );
        AutoDepositTrustedDestinationMutationResult result = repository.register(destination);
        String message = AutoDepositTrustedCommandFormatter.mutation(result)
                + ", source=" + target.get().source();
        if (result.success()) {
            mod.log(message);
        } else {
            mod.logWarning(message);
        }
    }
}
