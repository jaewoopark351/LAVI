package lavi.minecraft.task.container.home.command;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.tasksystem.Task;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

//20260827_kpopmodder: Start trusted home storage only from an explicit canonical user command.
public final class StoreHomeCommand extends Command {
    public static final String COMMAND_NAME = "store_home";

    private final Supplier<? extends Task> taskSupplier;
    private final Predicate<AltoClef> cursorGate;

    public StoreHomeCommand(StoreHomeTaskFactory taskFactory) throws CommandException {
        this(
                Objects.requireNonNull(taskFactory, "taskFactory")::create,
                StoreHomeCommand::cursorEmpty
        );
    }

    StoreHomeCommand(
            Supplier<? extends Task> taskSupplier,
            Predicate<AltoClef> cursorGate) throws CommandException {
        super(COMMAND_NAME, "Store manual surplus in registered trusted home containers.");
        this.taskSupplier = Objects.requireNonNull(taskSupplier, "taskSupplier");
        this.cursorGate = Objects.requireNonNull(cursorGate, "cursorGate");
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) {
        if (!cursorGate.test(mod)) {
            mod.logWarning("Store home: result=CURSOR_NOT_EMPTY; inventory was not changed.");
            finish();
            return;
        }
        mod.runUserTask(taskSupplier.get(), this::finish);
    }

    private static boolean cursorEmpty(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null
                || mod.getPlayer().currentScreenHandler == null) {
            return false;
        }
        ItemStack cursor = mod.getPlayer().currentScreenHandler.getCursorStack();
        return cursor == null || cursor.isEmpty();
    }
}
