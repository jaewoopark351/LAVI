//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.movement;
//$$ 
//$$ import java.util.function.BooleanSupplier;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ 
//$$ //20260914_kpopmodder: One explicit route start borrows the parent budget and owns no terminal result.
//$$ public interface FindExplorationMovementPort {
//$$     void begin(FindObservationPort.Binding admission, BooleanSupplier withinBudget);
//$$     Step tick();
//$$     void suspend();
//$$     boolean quiet();
//$$ 
//$$     record Progress(int completedSteps, int plannedSteps, int captureColumns, boolean movementInputIssued) { }
//$$     record Step(String reason, boolean waypointReached, boolean quiet, Progress progress) { }
//$$ }
//#endif
