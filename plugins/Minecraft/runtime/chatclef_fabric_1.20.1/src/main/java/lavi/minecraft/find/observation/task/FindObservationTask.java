//#if MC == 12001
//$$ package lavi.minecraft.find.observation.task;

//$$ import java.util.UUID;
//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationOperation;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;
//$$ import lavi.minecraft.find.result.FindPresentationBinding;
//$$ import net.minecraft.client.MinecraftClient;

//$$ //20260914_kpopmodder: Participate in the existing Task lifecycle without changing scheduler or generic cleanup.
//$$ public final class FindObservationTask extends Task implements FindTaskResultSource {
//$$     private final FindObservationOperation operation;
//$$     private final FindPresentationBinding presentation = new FindPresentationBinding();
//$$     public FindObservationTask(FindRequest request) {
//$$         String operationId = UUID.randomUUID().toString();
//$$         operation = new FindObservationOperation(request, operationId,
//$$                 new MinecraftFindObservationPort(MinecraftClient.getInstance()), System::nanoTime, new FindLog(operationId));
//$$     }
//$$     public FindObservationTask(FindObservationOperation operation) { this.operation = operation; }
//$$     public FindRequest request() { return operation.request(); }
//$$     public String operationId() { return operation.operationId(); }
//$$     public FindOutcome outcome() { return operation.outcome(); }
//$$     public void diagnosticRetired(String reason) { operation.diagnosticRetired(reason); }
//$$     public void suppressNativePresentation() { presentation.suppressNativePresentation(); }
//$$     public boolean nativePresentationSuppressed() { return presentation.nativePresentationSuppressed(); }
//$$     @Override protected void onStart() { operation.start(); }
//$$     @Override protected Task onTick() { operation.tick(); return null; }
//$$     @Override protected void onStop(Task interruptTask) { operation.suspend(); }
//$$     @Override public boolean isFinished() { return outcome() != null; }
//$$     @Override protected boolean isEqual(Task other) {
//$$         return other instanceof FindObservationTask task && task.request().equals(request());
//$$     }
//$$     @Override protected String toDebugString() { return "Find " + request().kind() + " " + request().mode(); }
//$$ }

//#endif
