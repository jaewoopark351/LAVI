//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.commandsystem.Arg;
//$$ import adris.altoclef.commandsystem.ArgParser;
//$$ import adris.altoclef.commandsystem.Command;
//$$ import adris.altoclef.commandsystem.CommandException;
//$$ import lavi.minecraft.find.approach.task.FindApproachTask;
//$$ import lavi.minecraft.find.observation.task.FindObservationTask;
//$$ import lavi.minecraft.find.exploration.task.FindExplorationTask;
//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;

//$$ //20260914_kpopmodder: FIND reuses command-owned user task execution without ATTACK or acquisition behavior.
//$$ public final class FindCommand extends Command {
//$$     private final FindNativeCompletionObserver nativeObserver = new FindNativeCompletionObserver();
//$$     public FindCommand() throws CommandException {
//$$         super("find", "Find a loaded mob, exact player, block or dropped item; report by default.",
//$$                 new Arg<>(String.class, "kind"), new Arg<>(String.class, "target"), new Arg<>(String.class, "mode", "report", 2));
//$$     }
//$$     @Override protected void call(AltoClef mod, ArgParser parser) throws CommandException {
//$$         FindNativeAdmission.requireAvailable(mod);
//$$         var request = FindRequestResolver.resolve(parser.get(String.class), parser.get(String.class), parser.get(String.class));
//$$         long admittedNanos = System.nanoTime();
//$$         Task task = request.kind().equals("entity") ? new FindExplorationTask(request, admittedNanos)
//$$                 : request.mode().equals("approach") ? new FindApproachTask(request) : new FindObservationTask(request);
//$$         nativeObserver.prepare(task, (FindTaskResultSource) task, mod);
//$$         boolean submitted = false;
//$$         try {
//$$             mod.runUserTask(task, this::finish);
//$$             nativeObserver.bind(mod.getUserTaskChain().currentRootLifetime());
//$$             submitted = true;
//$$         } finally {
//$$             if (!submitted) nativeObserver.retire("native_task_submission_failed");
//$$         }
//$$     }
//$$     public void onEndClientTick() { nativeObserver.onEndClientTick(); }
//$$ }

//#endif
