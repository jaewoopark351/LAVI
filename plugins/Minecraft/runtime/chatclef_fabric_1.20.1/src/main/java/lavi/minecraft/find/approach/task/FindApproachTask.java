//#if MC == 12001
//$$ package lavi.minecraft.find.approach.task;
//$$
//$$ import java.util.UUID;
//$$ import java.util.function.LongSupplier;
//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.approach.movement.FindApproachMovementPort;
//$$ import lavi.minecraft.find.approach.movement.MinecraftFindNativeMovement;
//$$ import lavi.minecraft.find.approach.operation.FindApproachOperation;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationOperation;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;
//$$ import net.minecraft.client.MinecraftClient;
//$$
//$$ //20260914_kpopmodder: Own one observed candidate and terminal; compose existing Task and native finite movement.
//$$ public final class FindApproachTask extends Task implements FindTaskResultSource {
//$$     public static final long DEADLINE_NANOS = FindApproachOperation.DEADLINE_NANOS;
//$$     private final FindApproachOperation operation;
//$$     private final boolean requireUserRoot;
//$$     private boolean nativePresentationSuppressed;
//$$
//$$     public FindApproachTask(FindRequest request) {
//$$         String id = UUID.randomUUID().toString();
//$$         LongSupplier clock = System::nanoTime; FindLog log = new FindLog(id);
//$$         FindObservationPort observations = new MinecraftFindObservationPort(MinecraftClient.getInstance());
//$$         var observation = new FindObservationOperation(request, id, observations, clock, log);
//$$         var movement = new MinecraftFindNativeMovement(MinecraftClient.getInstance(), log);
//$$         operation = new FindApproachOperation(observation, observations, movement, clock, log);
//$$         requireUserRoot = true;
//$$     }
//$$     public FindApproachTask(FindObservationOperation observation, FindObservationPort observations,
//$$                             FindApproachMovementPort movement, LongSupplier clock, FindLog log) {
//$$         operation = new FindApproachOperation(observation, observations, movement, clock, log);
//$$         requireUserRoot = false;
//$$     }
//$$     @Override protected void onStart() {
//$$         operation.start();
//$$     }
//$$     @Override protected Task onTick() {
//$$         operation.tick(!requireUserRoot || AltoClef.getInstance().getUserTaskChain().getCurrentTask() == this);
//$$         return null;
//$$     }
//$$     @Override protected void onStop(Task interruptTask) {
//$$         // SingleTaskChain also uses onStop(null) for temporary defense preemption.
//$$         operation.suspend();
//$$     }
//$$     @Override public boolean isFinished() { return outcome() != null; }
//$$     @Override public FindOutcome outcome() { return operation.outcome(); }
//$$     @Override public FindRequest request() { return operation.request(); }
//$$     @Override public String operationId() { return operation.operationId(); }
//$$     @Override public void diagnosticRetired(String reason) { operation.diagnosticRetired(reason); }
//$$     @Override public void suppressNativePresentation() { nativePresentationSuppressed = true; }
//$$     @Override public boolean nativePresentationSuppressed() { return nativePresentationSuppressed; }
//$$     @Override protected boolean isEqual(Task other) {
//$$         return other instanceof FindApproachTask task && request().equals(task.request());
//$$     }
//$$     @Override protected String toDebugString() { return "Find " + request().kind() + " approach"; }
//$$ }
//#endif
