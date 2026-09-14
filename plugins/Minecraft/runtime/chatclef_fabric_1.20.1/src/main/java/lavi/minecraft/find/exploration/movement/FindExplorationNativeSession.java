//#if MC == 12001
//$$ package lavi.minecraft.find.exploration.movement;
//$$ 
//$$ import java.util.Map;
//$$ import java.util.function.BooleanSupplier;
//$$ import baritone.api.utils.Rotation;
//$$ import baritone.api.utils.input.Input;
//$$ import lavi.minecraft.find.approach.input.FindOwnedMovementInputs;
//$$ import lavi.minecraft.find.observation.FindObservationPort;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$ 
//$$ //20260914_kpopmodder: Native decisions are staged; the route owner fences every eventual input/look effect.
//$$ interface FindExplorationNativeSession {
//$$     record Decision(String reason, Map<Input, Boolean> inputs, Rotation rotation, boolean stepCompleted) {
//$$         static Decision waiting() { return new Decision(null, Map.of(), null, false); }
//$$         static Decision rejected(String reason) { return new Decision(reason, Map.of(), null, false); }
//$$     }
//$$     boolean matches(FindObservationPort.Binding binding);
//$$     boolean supportedPlayer();
//$$     boolean contended();
//$$     boolean safeWaypoint(BlockPos waypoint);
//$$     boolean movementKeysFree(FindOwnedMovementInputs owned);
//$$     BlockPos playerBlock();
//$$     Vec3d playerPosition();
//$$     double horizontalVelocitySquared();
//$$     ForcedInputLeaseChannel inputChannel();
//$$     void start(FindObservationPort.Binding local, BlockPos start, BlockPos waypoint);
//$$     Decision tick(BooleanSupplier withinBudget);
//$$     void look(Rotation rotation);
//$$     void suspend();
//$$     boolean quiet();
//$$     FindExplorationMovementPort.Progress progress();
//$$ }
//#endif
