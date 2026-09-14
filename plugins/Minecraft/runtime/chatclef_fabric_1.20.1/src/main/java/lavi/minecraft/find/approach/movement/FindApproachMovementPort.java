//#if MC == 12001
//$$ package lavi.minecraft.find.approach.movement;
//$$
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$
//$$ //20260914_kpopmodder: The parent retains terminal ownership while this port owns only movement resources.
//$$ public interface FindApproachMovementPort {
//$$     void begin(FindObservationPort.Binding binding, FindRequest request, FindCandidate candidate);
//$$     Step tick();
//$$     void suspend();
//$$     boolean quiet();
//$$     default Progress progress() { return new Progress(-1, -1, -1, false); }
//$$     record Progress(int completedSteps, int plannedSteps, int captureColumns, boolean movementInputIssued) { }
//$$     record Step(String failure, boolean arrived, FindCandidate candidate) {
//$$         public static Step waiting() { return new Step(null, false, null); }
//$$     }
//$$ }
//#endif
