package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStatusInspector;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: List trusted registrations with conservative runtime status labels.
public final class AutoDepositTrustedListCommand extends Command {
    private static final int MAX_LISTED_DESTINATIONS = 64;

    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedDestinationStatusInspector statusInspector;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public AutoDepositTrustedListCommand(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedDestinationStatusInspector statusInspector,
            AutoDepositWorldKeyReader worldKeyReader) throws CommandException {
        super("auto_deposit_trusted_list", "List trusted automatic storage destinations.");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.statusInspector = Objects.requireNonNull(statusInspector, "statusInspector");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        try {
            List<AutoDepositTrustedDestination> destinations = repository.destinations().stream()
                    .sorted(Comparator.comparing(AutoDepositTrustedDestination::key))
                    .toList();
            if (destinations.isEmpty()) {
                mod.log("No trusted automatic-deposit destinations are registered.");
                return;
            }
            String worldKey = worldKeyReader.read().orElse(null);
            Dimension dimension = mod.getWorld() == null ? null : WorldHelper.getCurrentDimension();
            int listed = Math.min(MAX_LISTED_DESTINATIONS, destinations.size());
            mod.log("Trusted automatic-deposit destinations: " + destinations.size());
            for (int index = 0; index < listed; index++) {
                AutoDepositTrustedDestination destination = destinations.get(index);
                AutoDepositTrustedDestinationStatus status = statusInspector.inspect(
                        mod, destination, worldKey, dimension
                );
                mod.log(AutoDepositTrustedCommandFormatter.listing(destination, status));
            }
            if (listed < destinations.size()) {
                mod.logWarning("Trusted destination list truncated: "
                        + (destinations.size() - listed) + " additional registrations.");
            }
        } finally {
            finish();
        }
    }
}
