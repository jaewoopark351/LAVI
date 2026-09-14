//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.task;
//$$ 
//$$ import java.util.UUID;
//$$ import java.util.function.LongSupplier;
//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.approach.movement.MinecraftFindNativeMovement;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.exploration.movement.MinecraftFindExplorationMovement;
//$$ import lavi.minecraft.find.exploration.operation.FindExplorationLimits;
//$$ import lavi.minecraft.find.exploration.operation.FindExplorationOperation;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$ import lavi.minecraft.find.result.FindPresentationBinding;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;
//$$ import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;
//$$ import net.minecraft.client.MinecraftClient;
//$$ 
//$$ //20260914_kpopmodder: Existing user-root lifecycle runs one FIND parent through all discovery phases.
//$$ public final class FindExplorationTask extends Task implements FindTaskResultSource {
//$$     private final FindExplorationOperation operation;
//$$     private final FindLog log;
//$$     private UserRootLifetime rootLifetime;
//$$     private final FindPresentationBinding presentation = new FindPresentationBinding();
//$$     public FindExplorationTask(FindRequest request, long admittedNanos) {
//$$         String id = UUID.randomUUID().toString();
//$$         LongSupplier clock = System::nanoTime;
//$$         log = new FindLog(id);
//$$         var client = MinecraftClient.getInstance();
//$$         operation = new FindExplorationOperation(request, id, this,
//$$             new MinecraftFindObservationPort(client), new MinecraftFindExplorationMovement(client, log.inPhase("EXPLORATION_MOVEMENT")),
//$$             new MinecraftFindNativeMovement(client, log.inPhase("APPROACH_MOVEMENT"), () -> operationAllowsApproach()), clock, log,
//$$             admittedNanos, FindExplorationLimits.DEFAULT, this::rootMatches);
//$$     }
//$$     private boolean rootMatches() {
//$$         var mod = AltoClef.getInstance();
//$$         var chain = mod == null ? null : mod.getUserTaskChain();
//$$         return rootLifetime != null && rootLifetime.owns(this) && rootLifetime.completion() == null
//$$             && chain != null && chain.getCurrentTask() == this && chain.currentRootLifetime() == rootLifetime;
//$$     }
//$$     @Override public void bindUserRootLifetime(UserRootLifetime lifetime) {
//$$         if (operation.retired()) return;
//$$         boolean valid = lifetime != null && lifetime.owns(this) && lifetime.completion() == null;
//$$         boolean accepted = valid && (rootLifetime == null || rootLifetime == lifetime);
//$$         String reason = !valid ? "invalid_lifetime" : !accepted ? "rejected_rebinding"
//$$             : rootLifetime == null ? "accepted_first_binding" : "already_same_binding";
//$$         if (accepted && rootLifetime == null) rootLifetime = lifetime;
//$$         log.event("ROOT_LIFETIME_BINDING_RESULT", "reason", reason, "accepted", accepted,
//$$             "firstBindingPreserved", rootLifetime != null);
//$$         if (!accepted) operation.retireOwnedResources("root_lifetime_binding_rejected");
//$$     }
//$$     private boolean operationAllowsApproach() { return rootMatches() && operation.approachMovementAllowed(); }
//$$     @Override protected void onStart() { operation.start(); }
//$$     @Override protected Task onTick() { operation.tick(rootMatches()); return null; }
//$$     @Override protected void onStop(Task interruptTask) { operation.suspend(); }
//$$     @Override public boolean isFinished() { return outcome() != null || operation.retired() && operation.ownedQuiet(); }
//$$     @Override public FindRequest request() { return operation.request(); }
//$$     @Override public String operationId() { return operation.operationId(); }
//$$     @Override public FindOutcome outcome() { return operation.outcome(); }
//$$     @Override public void observeClientTick() { operation.observeClientTick(rootMatches()); }
//$$     @Override public void retireOwnedResources(String reason) { operation.retireOwnedResources(reason); }
//$$     @Override public void diagnosticRetired(String reason) { operation.diagnosticRetired(reason); }
//$$     @Override public void suppressNativePresentation() { presentation.suppressNativePresentation(); }
//$$     @Override public boolean nativePresentationSuppressed() { return presentation.nativePresentationSuppressed(); }
//$$     @Override protected boolean isEqual(Task other) { return other == this; }
//$$     @Override protected String toDebugString() { return "Find discovery " + request().kind() + " " + request().mode(); }
//$$ }
//#endif
