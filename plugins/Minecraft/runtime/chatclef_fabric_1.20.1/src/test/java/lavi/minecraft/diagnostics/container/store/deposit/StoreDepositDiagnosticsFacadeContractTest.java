package lavi.minecraft.diagnostics.container.store.deposit;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260829_kpopmodder: Characterize the compatibility facade before extracting additional store-deposit event families.
class StoreDepositDiagnosticsFacadeContractTest {
    @Test
    void preservesTheExactPublicStaticEntryPointsUsedByExistingCallSites() {
        Set<String> expected = Set.of(
                "beginAutomaticRun(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,long,adris.altoclef.tasksystem.Task)->void",
                "beginAutomaticSlotAction(adris.altoclef.tasksystem.Task,java.lang.Object,int,int,int,net.minecraft.screen.slot.SlotActionType,net.minecraft.item.ItemStack)->void",
                "beginAutomaticSlotMutation(int,adris.altoclef.util.slots.Slot,net.minecraft.item.ItemStack,net.minecraft.item.ItemStack)->void",
                "beginDepositAllParentFilteredSearchObservation(adris.altoclef.tasksystem.Task,net.minecraft.util.math.BlockPos)->void",
                "beginFilteredSearchObservation(adris.altoclef.tasksystem.Task)->void",
                "bindInteraction(adris.altoclef.tasksystem.Task,lavi.minecraft.diagnostics.interaction.BlockInteractionContext)->void",
                "bindRootTracker(adris.altoclef.tasksystem.Task,adris.altoclef.tasks.container.ContainerStoredTracker)->void",
                "bindTargetTracker(adris.altoclef.tasksystem.Task,adris.altoclef.tasks.container.ContainerStoredTracker,net.minecraft.util.math.BlockPos)->void",
                "clearPredicateSnapshot()->void",
                "endDepositAllParentFilteredSearchObservation(adris.altoclef.tasksystem.Task,boolean)->void",
                "endFilteredSearchObservation(adris.altoclef.tasksystem.Task,boolean,net.minecraft.block.Block[])->void",
                "endAutomaticSlotAction()->void",
                "endAutomaticSlotMutation()->void",
                "hasAutomaticTaskContext(adris.altoclef.tasksystem.Task)->boolean",
                "hasAutomaticTrackerContext(adris.altoclef.tasks.container.ContainerStoredTracker)->boolean",
                "interactionFields(lavi.minecraft.diagnostics.interaction.BlockInteractionContext)->java.lang.Object[]",
                "interactionScopeKey(lavi.minecraft.diagnostics.interaction.BlockInteractionContext)->java.lang.String",
                "logChildReconciliation(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,boolean,boolean,boolean,boolean,boolean,adris.altoclef.tasksystem.Task)->void",
                "logDepositAllParentCandidateDecision(adris.altoclef.tasksystem.Task,java.lang.String,boolean,net.minecraft.util.math.BlockPos,boolean,boolean,boolean,boolean,net.minecraft.util.math.BlockPos,adris.altoclef.util.ItemTarget[],java.lang.Object[])->void",
                "logEffectObservation(adris.altoclef.tasks.container.ContainerStoredTracker,adris.altoclef.util.slots.Slot,net.minecraft.item.ItemStack,net.minecraft.item.ItemStack,boolean,boolean,boolean)->void",
                "logEffectObservation(adris.altoclef.tasks.container.ContainerStoredTracker,adris.altoclef.util.slots.Slot,net.minecraft.item.ItemStack,net.minecraft.item.ItemStack,boolean,boolean,boolean,java.lang.String,java.lang.String)->void",
                "logFilteredSearchResult(adris.altoclef.tasksystem.Task,java.util.Optional,net.minecraft.block.Block[])->void",
                "logNaturalFinish(adris.altoclef.tasksystem.Task)->void",
                "logParentCandidateDecision(adris.altoclef.tasksystem.Task,java.lang.String,net.minecraft.util.math.BlockPos,boolean,boolean,net.minecraft.util.math.BlockPos,adris.altoclef.util.ItemTarget[],java.lang.Object[])->void",
                "logPursuitDecision(adris.altoclef.tasksystem.Task,java.lang.Object,java.lang.Object,java.lang.String)->void",
                "logTargetCallbackDecision(adris.altoclef.tasksystem.Task,net.minecraft.util.math.BlockPos,net.minecraft.util.math.BlockPos,boolean,boolean,adris.altoclef.util.ItemTarget[])->void",
                "logTaskLifecycleBoundary(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,java.lang.String,java.lang.String,boolean)->void",
                "logTransferDecision(adris.altoclef.tasksystem.Task,net.minecraft.util.math.BlockPos,adris.altoclef.util.ItemTarget,int,boolean,boolean,boolean,java.lang.String)->void",
                "logUserBlockRangeNullInput(java.lang.Object,net.minecraft.util.math.BlockPos)->void",
                "markExplicitCancelCandidate(adris.altoclef.tasksystem.Task)->void",
                "observeAutomaticMovementResult(adris.altoclef.tasksystem.Task,net.minecraft.util.math.BlockPos,java.lang.String,boolean,boolean,boolean)->void",
                "observeAutomaticNotStoredDecision(adris.altoclef.tasks.container.ContainerStoredTracker,adris.altoclef.util.ItemTarget,int,boolean,boolean,boolean,int,boolean,int,adris.altoclef.util.ItemTarget)->void",
                "observeAutomaticSlotActionReturn(net.minecraft.item.ItemStack)->void",
                "observeContainerRouteEvent(java.lang.String,java.lang.String,adris.altoclef.tasksystem.Task,java.lang.Object[])->void",
                "observeDepositAllContainerEligibility(net.minecraft.util.math.BlockPos,java.lang.String)->void",
                "observeTargetContainerPredicate(adris.altoclef.tasks.container.ContainerStoredTracker,adris.altoclef.util.slots.Slot,net.minecraft.util.math.BlockPos,java.util.Optional,boolean)->void",
                "onStoreRootStart(adris.altoclef.tasksystem.Task,boolean,adris.altoclef.util.ItemTarget[])->java.lang.Object[]",
                "onStoreRootStopCallback(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task)->java.lang.Object[]",
                "recordAutomaticMaintenanceTerminal(adris.altoclef.tasksystem.Task,java.lang.String,java.lang.String)->void",
                "recordAutomaticPressureRunClosed(adris.altoclef.tasksystem.Task,java.lang.String,java.lang.String)->void",
                "registerBareDepositInvocation(adris.altoclef.AltoClef,boolean,adris.altoclef.util.ItemTarget[],adris.altoclef.tasksystem.Task)->java.lang.Object[]",
                "registerBareDepositInvocation(adris.altoclef.AltoClef,boolean,adris.altoclef.util.ItemTarget[],adris.altoclef.tasksystem.Task,java.lang.String)->java.lang.Object[]",
                "registerAutomaticMaintenanceChild(adris.altoclef.AltoClef,adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,int,adris.altoclef.util.ItemTarget[])->void",
                "shouldEmitInteractionDetail(lavi.minecraft.diagnostics.interaction.BlockInteractionContext,java.lang.String,java.lang.String)->boolean",
                "stageAutomaticRouteCandidate(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,net.minecraft.util.math.BlockPos,int,boolean)->void",
                "stageAutomaticTransferCandidate(adris.altoclef.tasksystem.Task,adris.altoclef.tasksystem.Task,lavi.minecraft.diagnostics.container.store.deposit.transfer.StoreDepositTransferSelectionSnapshot)->void",
                "trackerSubscriptionStarted(adris.altoclef.tasks.container.ContainerStoredTracker)->void",
                "trackerSubscriptionStopped(adris.altoclef.tasks.container.ContainerStoredTracker)->void"
        );

        Set<String> actual = Arrays.stream(StoreDepositDiagnostics.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(StoreDepositDiagnosticsFacadeContractTest::signature)
                .collect(Collectors.toSet());

        assertEquals(expected, actual);
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
