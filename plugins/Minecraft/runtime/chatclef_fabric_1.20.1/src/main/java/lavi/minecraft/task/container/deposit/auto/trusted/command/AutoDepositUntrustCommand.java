package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;

import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Remove trusted storage by stable ID or the same exact-target rule as registration.
public final class AutoDepositUntrustCommand extends Command {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositTrustedTargetResolver targetResolver;
    private final AutoDepositWorldKeyReader worldKeyReader;

    public AutoDepositUntrustCommand(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositTrustedTargetResolver targetResolver,
            AutoDepositWorldKeyReader worldKeyReader) throws CommandException {
        super(
                "auto_deposit_untrust",
                "Remove a trusted automatic storage destination by target or stable ID.",
                new Arg(String.class, "destinationId", null, 0, false)
        );
        this.repository = Objects.requireNonNull(repository, "repository");
        this.targetResolver = Objects.requireNonNull(targetResolver, "targetResolver");
        this.worldKeyReader = Objects.requireNonNull(worldKeyReader, "worldKeyReader");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        try {
            String destinationId = parser.get(String.class);
            AutoDepositTrustedDestinationMutationResult result;
            if (destinationId != null && !destinationId.isBlank()) {
                result = repository.unregisterById(destinationId);
            } else {
                Optional<String> worldKey = worldKeyReader.read();
                Optional<AutoDepositTrustedTarget> target = targetResolver.resolve(mod);
                if (worldKey.isEmpty() || target.isEmpty() || mod.getWorld() == null) {
                    lavi.minecraft.command.result.instant.InstantCommandResultCapture.record("auto_deposit_untrust", false,
                            "TARGET_UNAVAILABLE", java.util.Map.of());
                    mod.logWarning("Trusted destination not removed: provide a destination ID or target a supported container.");
                    return;
                }
                AutoDepositTrustedDestination destination = new AutoDepositTrustedDestination(
                        worldKey.get(),
                        WorldHelper.getCurrentDimension(),
                        target.get().position(),
                        true
                );
                result = repository.unregister(destination);
            }
            String message = AutoDepositTrustedCommandFormatter.mutation(result);
            //20260915_kpopmodder: Preserve native persistence decisions in the exact invoking command result.
            lavi.minecraft.command.result.instant.InstantCommandResultCapture.record("auto_deposit_untrust", result.success(),
                    result.status().name(), java.util.Map.of("destination_id",
                            result.destination().map(AutoDepositTrustedDestination::destinationId).orElse("")));
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
