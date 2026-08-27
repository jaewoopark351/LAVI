package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Register only a user-selected exact container as trusted automatic storage.
public final class AutoDepositTrustCommand extends Command {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedTargetResolver targetResolver;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public AutoDepositTrustCommand(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedTargetResolver targetResolver,
            AutoDepositWorldKeyReader worldKeyReader) throws CommandException {
        super("auto_deposit_trust", "Register the open or targeted container as trusted automatic storage.");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        try {
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
        } finally {
            finish();
        }
    }
}
