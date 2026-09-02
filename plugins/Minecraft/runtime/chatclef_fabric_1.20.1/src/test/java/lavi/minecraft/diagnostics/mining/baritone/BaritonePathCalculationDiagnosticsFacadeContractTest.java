package lavi.minecraft.diagnostics.mining.baritone;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize the Baritone calculation facade before any further collaborator extraction.
class BaritonePathCalculationDiagnosticsFacadeContractTest {
    @Test
    void preservesTheExactPublicStaticEntryPointsUsedByDiagnosticMixins() {
        Set<String> expected = Set.of(
                "logAdoptionDecisionBeforeClear(baritone.behavior.PathingBehavior,boolean,net.minecraft.util.math.BlockPos,baritone.api.pathing.goals.Goal,baritone.pathing.calc.AbstractNodeCostSearch,baritone.pathing.path.PathExecutor,baritone.pathing.path.PathExecutor,baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.utils.BetterBlockPos)->void",
                "logCalculate0Enter(baritone.pathing.calc.AbstractNodeCostSearch,long,long)->void",
                "logCalculate0Return(baritone.pathing.calc.AbstractNodeCostSearch,java.util.Optional)->void",
                "logCalculationScheduled(baritone.behavior.PathingBehavior,net.minecraft.util.math.BlockPos,boolean,baritone.pathing.movement.CalculationContext,baritone.api.pathing.goals.Goal,baritone.pathing.path.PathExecutor,baritone.pathing.path.PathExecutor,baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.utils.BetterBlockPos)->void",
                "logForceCancelBoundary(baritone.behavior.PathingBehavior,java.lang.String,baritone.pathing.path.PathExecutor,baritone.pathing.path.PathExecutor,baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.pathing.goals.Goal,boolean,boolean)->void",
                "logGoalRequestDecision(baritone.behavior.PathingBehavior,baritone.api.process.PathingCommand,boolean,baritone.pathing.path.PathExecutor,baritone.pathing.path.PathExecutor,baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.pathing.goals.Goal,baritone.api.utils.BetterBlockPos,boolean,boolean)->void",
                "logPathBuildEnter(java.lang.Object,baritone.api.utils.BetterBlockPos,baritone.pathing.calc.PathNode,baritone.pathing.calc.PathNode,int,baritone.api.pathing.goals.Goal,baritone.pathing.movement.CalculationContext)->void",
                "logPathBuildReturn(java.lang.Object,baritone.api.utils.BetterBlockPos,baritone.pathing.calc.PathNode,baritone.pathing.calc.PathNode,int,baritone.api.pathing.goals.Goal,baritone.pathing.movement.CalculationContext)->void",
                "logPathPostProcessEnter(baritone.api.pathing.calc.IPath)->void",
                "logPathPostProcessReturn(baritone.api.pathing.calc.IPath,baritone.api.pathing.calc.IPath)->void",
                "logPathfinderCalculateCompleted(baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.utils.PathCalculationResult,long,boolean,java.lang.Object[])->void",
                "logPathfinderCalculateCompleted(baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.utils.PathCalculationResult,long,boolean,java.util.function.Supplier)->void",
                "logPathfinderCalculateStarted(baritone.pathing.calc.AbstractNodeCostSearch,long,long,baritone.api.pathing.goals.Goal,baritone.api.utils.BetterBlockPos,boolean)->void",
                "logWorkerStarted(baritone.behavior.PathingBehavior,boolean,net.minecraft.util.math.BlockPos,baritone.api.pathing.goals.Goal,baritone.pathing.calc.AbstractNodeCostSearch,long,long,baritone.pathing.path.PathExecutor,baritone.pathing.path.PathExecutor,baritone.pathing.calc.AbstractNodeCostSearch,baritone.api.utils.BetterBlockPos)->void"
        );

        Set<String> actual = Arrays.stream(BaritonePathCalculationDiagnostics.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(BaritonePathCalculationDiagnosticsFacadeContractTest::signature)
                .collect(Collectors.toSet());

        assertEquals(expected, actual);
    }

    @Test
    void remainsANonInstantiableFinalCompatibilityFacade() {
        assertTrue(Modifier.isPublic(BaritonePathCalculationDiagnostics.class.getModifiers()));
        assertTrue(Modifier.isFinal(BaritonePathCalculationDiagnostics.class.getModifiers()));

        Constructor<?>[] constructors = BaritonePathCalculationDiagnostics.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    private static String signature(Method method) {
        return method.getName()
                + "("
                + Arrays.stream(method.getParameterTypes())
                .map(Class::getTypeName)
                .collect(Collectors.joining(","))
                + ")->"
                + method.getReturnType().getTypeName();
    }
}
