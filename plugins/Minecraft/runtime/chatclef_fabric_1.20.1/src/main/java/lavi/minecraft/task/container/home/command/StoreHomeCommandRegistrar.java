package lavi.minecraft.task.container.home.command;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.CommandException;

import java.util.Objects;

//20260827_kpopmodder: Register the LAVI-owned manual command without editing upstream command lists.
public final class StoreHomeCommandRegistrar {
    private final StoreHomeTaskFactory taskFactory;
    private boolean registrationAttempted;
    private boolean registered;

    public StoreHomeCommandRegistrar(StoreHomeTaskFactory taskFactory) {
        this.taskFactory = Objects.requireNonNull(taskFactory, "taskFactory");
    }

    public void register(AltoClef mod) {
        if (registered || registrationAttempted) {
            return;
        }
        if (mod == null || mod.getCommandExecutor() == null) {
            return;
        }
        registrationAttempted = true;
        if (mod.getCommandExecutor().get(StoreHomeCommand.COMMAND_NAME) != null) {
            Debug.logError("Store-home command name already registered; registration refused.");
            return;
        }
        try {
            mod.getCommandExecutor().registerNewCommand(new StoreHomeCommand(taskFactory));
            registered = true;
        } catch (CommandException exception) {
            Debug.logError("Failed to register store-home command: " + exception.getMessage());
        }
    }

    public boolean registered() {
        return registered;
    }
}
