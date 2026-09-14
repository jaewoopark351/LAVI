//#if MC == 12001
//20260914_kpopmodder: Register an attack-like command whose root task observes/approaches but never kills or gathers.
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandException;

public final class FindCommand extends Command {
    public FindCommand() throws CommandException {
        super("find", "Find a loaded entity, placed block, or dropped item. @find [entity|block|item|player] target [approach|report]",
                new Arg<>(String.class, "target").asArray());
    }

    @Override public void run(AltoClef mod, String line, Runnable onFinish) throws CommandException {
        // Parse before ArgParser drops comments; retain this invocation's callback, not Command's mutable callback field.
        start(mod, line, onFinish);
    }
    @Override protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        start(mod, "find " + String.join(" ", parser.getArgUnits()), this::finish);
    }
    private void start(AltoClef mod, String line, Runnable onFinish) throws CommandException {
        final FindRequest request;
        try { request = FindRequest.parse(line); }
        catch (IllegalArgumentException invalid) { throw new CommandException(invalid.getMessage()); }
        var task = new FindTask(request);
        mod.runUserTask(task, () -> {
            // The bridge callback must run even if native chat rendering fails.
            try {
                if (task.outcome() != null) Debug.logMessage(task.outcome().koreanMessage());
            } finally {
                if (onFinish != null) onFinish.run();
            }
        });
    }
}
//#endif
