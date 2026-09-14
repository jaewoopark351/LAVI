//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.Debug;
//$$ import adris.altoclef.commandsystem.CommandException;

//$$ //20260914_kpopmodder: Register FIND only after the existing engine executor is ready; never replace conflicts.
//$$ public final class FindCommandRegistrar {
//$$     private boolean registered;
//$$     private boolean failed;
//$$     private FindCommand command;
//$$     public void onEndClientTick() {
//$$         if (registered) { command.onEndClientTick(); return; }
//$$         if (failed) return;
//$$         try {
//$$             var executor = AltoClef.getCommandExecutor();
//$$             if (executor == null) return;
//$$             var existing = executor.get("find");
//$$             if (existing != null && !(existing instanceof FindCommand)) {
//$$                 failed = true;
//$$                 log(true, "LAVI FIND registration rejected operation=UNBOUND reason=command_name_conflict");
//$$                 return;
//$$             }
//$$             command = existing == null ? new FindCommand() : (FindCommand) existing;
//$$             if (existing == null) executor.registerNewCommand(command);
//$$             registered = true;
//$$             log(false, "LAVI FIND registration complete operation=UNBOUND command=find");
//$$         } catch (CommandException | RuntimeException | LinkageError error) {
//$$             failed = true;
//$$             log(true, "LAVI FIND registration failed operation=UNBOUND exceptionType=" + error.getClass().getSimpleName());
//$$         }
//$$     }
//$$     private static void log(boolean warning, String message) {
//$$         try { if (warning) Debug.logWarning(message); else Debug.logInternal(message); }
//$$         catch (RuntimeException | LinkageError ignored) { }
//$$     }
//$$ }

//#endif
