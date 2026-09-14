//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.Debug;
//$$ import adris.altoclef.commandsystem.CommandException;

//$$ //20260914_kpopmodder: Native FIND admission preserves an active non-idle user root before constructing another operation.
//$$ public final class FindNativeAdmission {
//$$     private FindNativeAdmission() { }
//$$     public static void requireAvailable(AltoClef mod) throws CommandException {
//$$         var chain = mod == null ? null : mod.getUserTaskChain();
//$$         if (chain == null) throw new CommandException("FIND INVALID_TARGET: user_task_chain_unavailable");
//$$         boolean active = chain.isActive();
//$$         boolean idle = chain.isRunningIdleTask();
//$$         requireAvailable(active, idle);
//$$     }
//$$     static void requireAvailable(boolean active, boolean idle) throws CommandException {
//$$         boolean accepted = !active || idle;
//$$         try { Debug.logInternal("LAVI FIND native admission active=" + active + " idle=" + idle + " accepted=" + accepted
//$$                 + " operation=UNBOUND reason=" + (accepted ? "user_root_available" : "non_idle_user_root_busy")); }
//$$         catch (RuntimeException | LinkageError ignored) { }
//$$         if (!accepted) throw new CommandException("FIND BUSY: non_idle_user_root_busy");
//$$     }
//$$ }
//#endif
