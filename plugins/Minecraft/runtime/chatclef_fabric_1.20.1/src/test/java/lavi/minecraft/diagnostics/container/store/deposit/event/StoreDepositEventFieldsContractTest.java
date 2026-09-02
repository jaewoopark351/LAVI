package lavi.minecraft.diagnostics.container.store.deposit.event;

import adris.altoclef.tasks.container.ContainerStoredTracker;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.container.store.deposit.binding.StoreDepositBindingRegistry.TrackerBinding;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Lock the ordered StoreDepositEventFields payload and public facade contracts before extraction.
class StoreDepositEventFieldsContractTest {
    private static final List<String> UNAVAILABLE_OPERATION_KEYS = List.of(
            "storeContextAvailable",
            "storeOperationId",
            "requestSource"
    );

    @Test
    void publicStaticFacadeSignaturesRemainExact() {
        Set<MethodSignature> expected = Set.of(
                signature("operationFields", Object[].class, StoreDepositOperationState.class),
                signature(
                        "automaticIdentityFields",
                        Object[].class,
                        StoreDepositAutomaticContext.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class
                ),
                signature("activeRouteIdentityFields", Object[].class, StoreDepositOperationState.class),
                signature(
                        "rootActivationFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        ItemTarget[].class
                ),
                signature(
                        "lifecycleFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        Task.class,
                        String.class,
                        String.class,
                        String.class,
                        boolean.class,
                        boolean.class,
                        boolean.class
                ),
                signature(
                        "childReconciliationFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        Task.class,
                        Task.class,
                        Task.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        String.class,
                        String.class
                ),
                signature(
                        "parentCandidateDecisionFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        String.class,
                        BlockPos.class,
                        boolean.class,
                        boolean.class,
                        BlockPos.class,
                        ItemTarget[].class,
                        Object[].class
                ),
                signature(
                        "filteredSearchResultFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        Optional.class,
                        Block[].class
                ),
                signature(
                        "pursuitDecisionFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        Object.class,
                        Object.class,
                        String.class
                ),
                signature(
                        "targetCallbackDecisionFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        BlockPos.class,
                        BlockPos.class,
                        boolean.class,
                        boolean.class,
                        ItemTarget[].class
                ),
                signature(
                        "craftRouteEventFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        String.class,
                        String.class,
                        Object[].class
                ),
                signature(
                        "transferDecisionFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Task.class,
                        BlockPos.class,
                        ItemTarget.class,
                        int.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        String.class
                ),
                signature(
                        "effectObservationFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        TrackerBinding.class,
                        ContainerStoredTracker.class,
                        Slot.class,
                        ItemStack.class,
                        ItemStack.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        String.class,
                        boolean.class,
                        BlockPos.class,
                        String.class
                ),
                signature(
                        "effectObservationFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        TrackerBinding.class,
                        ContainerStoredTracker.class,
                        Slot.class,
                        ItemStack.class,
                        ItemStack.class,
                        boolean.class,
                        boolean.class,
                        boolean.class,
                        String.class,
                        boolean.class,
                        BlockPos.class,
                        String.class,
                        boolean.class
                ),
                signature(
                        "terminalSummaryFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        String.class,
                        String.class,
                        Object[].class
                ),
                signature(
                        "effectSummaryFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        String.class
                ),
                signature(
                        "baritoneSummaryFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        String.class
                ),
                signature(
                        "coverageSummaryFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        String.class,
                        Object[].class
                ),
                signature(
                        "terminalReserveExhaustedFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        String.class,
                        Object[].class
                ),
                signature(
                        "exceptionFields",
                        Object[].class,
                        StoreDepositOperationState.class,
                        Object.class,
                        BlockPos.class,
                        String.class
                ),
                signature("merge", Object[].class, Object[].class, Object[].class),
                signature("operationId", String.class, StoreDepositOperationState.class),
                signature("identity", String.class, Object.class)
        );

        Set<MethodSignature> actual = new LinkedHashSet<>();
        for (Method method : StoreDepositEventFields.class.getDeclaredMethods()) {
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toGenericString());
            actual.add(new MethodSignature(
                    method.getName(),
                    method.getReturnType(),
                    List.copyOf(Arrays.asList(method.getParameterTypes()))
            ));
        }

        assertEquals(expected, actual);
        assertTrue(Modifier.isFinal(StoreDepositEventFields.class.getModifiers()));
        Constructor<?>[] constructors = StoreDepositEventFields.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPrivate(constructors[0].getModifiers()));
    }

    @Test
    void operationAndAutomaticIdentityBuildersPreserveOrderAvailabilityAndOmission() {
        assertArrayEquals(new Object[]{
                "storeContextAvailable", false,
                "storeOperationId", "UNAVAILABLE",
                "requestSource", "UNAVAILABLE"
        }, StoreDepositEventFields.operationFields(null));
        assertEquals("UNAVAILABLE", StoreDepositEventFields.operationId(null));
        assertEquals("none", StoreDepositEventFields.identity(null));

        StoreDepositOperationState manual = state("BARE_DEPOSIT_COMMAND");
        assertKeys(StoreDepositEventFields.operationFields(manual),
                "storeContextAvailable",
                "storeOperationId",
                "requestSource",
                "storeRootTaskIdentity",
                "storeRootTaskClass",
                "storeOperationStartTick",
                "storeOperationStartEventSequence"
        );
        assertEquals(List.of(), keys(StoreDepositEventFields.activeRouteIdentityFields(null)));
        assertEquals(List.of(), keys(StoreDepositEventFields.activeRouteIdentityFields(manual)));

        Object[] unavailableAutomatic = StoreDepositEventFields.automaticIdentityFields(
                null,
                null,
                "",
                " ",
                null,
                "",
                " ",
                null
        );
        assertArrayEquals(new Object[]{
                "autoContextAvailable", false,
                "autoOperationEpoch", "UNAVAILABLE",
                "policyContextEpoch", "UNAVAILABLE",
                "autoOperationId", "UNAVAILABLE",
                "maintenanceGenerationId", "UNAVAILABLE",
                "autoChildOperationId", "UNAVAILABLE",
                "autoChildIndex", "UNAVAILABLE",
                "pressureOwnedRunId", "UNAVAILABLE",
                "storeOperationId", "UNAVAILABLE",
                "selectedCandidateGenerationId", "UNAVAILABLE",
                "storeAttemptId", "UNAVAILABLE",
                "routeChildLifecycleId", "UNAVAILABLE",
                "transferAttemptId", "UNAVAILABLE",
                "slotActionId", "UNAVAILABLE",
                "slotMutationId", "UNAVAILABLE"
        }, unavailableAutomatic);

        StoreDepositOperationState automatic = state("AUTO_DEPOSIT_ALL_CHAIN");
        automatic.attachAutomaticContext(new StoreDepositAutomaticContext(
                true,
                11L,
                12L,
                "auto-11",
                "maintenance-12",
                "child-13",
                4,
                "pressure-14"
        ));
        String[] automaticOperationKeys = {
                "storeContextAvailable",
                "storeOperationId",
                "requestSource",
                "storeRootTaskIdentity",
                "storeRootTaskClass",
                "storeOperationStartTick",
                "storeOperationStartEventSequence",
                "autoContextAvailable",
                "autoOperationEpoch",
                "policyContextEpoch",
                "autoOperationId",
                "maintenanceGenerationId",
                "autoChildOperationId",
                "autoChildIndex",
                "pressureOwnedRunId",
                "selectedCandidateGenerationId",
                "storeAttemptId",
                "routeChildLifecycleId",
                "transferAttemptId",
                "slotActionId",
                "slotMutationId"
        };
        assertKeys(StoreDepositEventFields.operationFields(automatic), automaticOperationKeys);
        assertKeys(StoreDepositEventFields.activeRouteIdentityFields(automatic), automaticOperationKeys);
        assertEquals("UNAVAILABLE", field(
                StoreDepositEventFields.operationFields(automatic),
                "selectedCandidateGenerationId"
        ));
    }

    @Test
    void lifecycleFamilyBuildersPreserveOrderedKeys() {
        assertNullStateFamilyKeys(
                StoreDepositEventFields.rootActivationFields(null, null),
                "storeActivationEpoch",
                "activationKind",
                "requestedTargetItems"
        );

        assertNullStateFamilyKeys(
                StoreDepositEventFields.lifecycleFields(
                        null,
                        null,
                        null,
                        "STOP",
                        "END",
                        "ROOT",
                        true,
                        false,
                        true
                ),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "lifecycleAction",
                "lifecyclePhase",
                "taskActiveBefore",
                "lifecycleTaskRole",
                "interruptTaskInstanceId",
                "interruptTaskClass",
                "onStopCallbackExpected",
                "terminalPending",
                "operationFinalized",
                "taskSummary"
        );

        Object[] reconciliation = StoreDepositEventFields.childReconciliationFields(
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                true,
                true,
                true,
                "ROOT_CHILD",
                "TICK"
        );
        assertNullStateFamilyKeys(
                reconciliation,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "lifecycleTaskRole",
                "reconciliationRole",
                "reconciliationOutcome",
                "parentTask",
                "activeChildBefore",
                "candidateChild",
                "activeChildAfter",
                "subTasksEqual",
                "canInterruptEvaluated",
                "canInterruptPreviousChild",
                "replacementApplied",
                "previousChildStopCalled"
        );
        assertEquals("ACTIVE_CHILD_CLEARED", field(reconciliation, "reconciliationOutcome"));
    }

    @Test
    void routeFamilyBuildersPreserveOrderedKeysAndUnavailableRepresentations() {
        Object[] parentCandidate = StoreDepositEventFields.parentCandidateDecisionFields(
                null,
                null,
                "NO_CANDIDATE",
                null,
                false,
                false,
                null,
                null,
                null
        );
        assertNullStateFamilyKeys(
                parentCandidate,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "selectedBranch",
                "rawClosestContainerPresent",
                "rawClosestContainerPosition",
                "closestWithinRange",
                "currentTryWithinExtraRange",
                "currentChestTry",
                "notStoredTargets",
                "taskSummary"
        );

        Object[] nullSearch = StoreDepositEventFields.filteredSearchResultFields(
                null,
                null,
                null,
                null
        );
        assertNullStateFamilyKeys(
                nullSearch,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "filteredResultPresent",
                "filteredResultPosition",
                "targetBlocks",
                "taskSummary"
        );
        assertEquals(false, field(nullSearch, "filteredResultPresent"));
        assertEquals("unavailable", field(nullSearch, "filteredResultPosition"));
        assertEquals("null", field(nullSearch, "targetBlocks"));
        Object[] emptySearch = StoreDepositEventFields.filteredSearchResultFields(
                null,
                null,
                Optional.empty(),
                null
        );
        assertEquals("none", field(emptySearch, "filteredResultPosition"));

        Object[] pursuit = StoreDepositEventFields.pursuitDecisionFields(
                null,
                null,
                null,
                null,
                "NO_TARGET"
        );
        assertNullStateFamilyKeys(
                pursuit,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "currentPursuit",
                "candidate",
                "returnedAction",
                "taskSummary"
        );
        assertEquals("unavailable", field(pursuit, "currentPursuit"));
        assertEquals("unavailable", field(pursuit, "candidate"));

        assertNullStateFamilyKeys(
                StoreDepositEventFields.targetCallbackDecisionFields(
                        null,
                        null,
                        null,
                        null,
                        false,
                        true,
                        null
                ),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "callbackTarget",
                "currentChestTryBefore",
                "filteredTargetSameReferenceAsCurrentTry",
                "progressResetBecauseReferenceChanged",
                "activeChildBoundNotStoredTargets",
                "taskSummary"
        );

        Object[] craftRoute = StoreDepositEventFields.craftRouteEventFields(
                null,
                null,
                "ROUTE_DECISION",
                "NO_TABLE",
                null
        );
        assertNullStateFamilyKeys(
                craftRoute,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "observedEventName",
                "observedReason",
                "decision",
                "costToWalk",
                "costToMakeNew",
                "nearestPresent",
                "nearestPosition",
                "cachedContainerPosition",
                "openTableTask",
                "taskSummary"
        );
        assertEquals("unavailable", field(craftRoute, "decision"));
        assertEquals("unavailable", field(craftRoute, "openTableTask"));
    }

    @Test
    void transferAndEffectFamilyBuildersPreserveOrderedKeysAndOptionalSliceOmission() {
        assertNullStateFamilyKeys(
                StoreDepositEventFields.transferDecisionFields(
                        null,
                        null,
                        null,
                        null,
                        0,
                        false,
                        false,
                        false,
                        "NO_SOURCE"
                ),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "transferAction",
                "targetContainerPosition",
                "targetItem",
                "potentialSourceSlotCount",
                "bestSourcePresent",
                "destinationSlotEvaluated",
                "destinationSlotPresent",
                "taskSummary"
        );

        Object[] baseEffect = StoreDepositEventFields.effectObservationFields(
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "UNAVAILABLE",
                false,
                null,
                "NO_CHANGE",
                false
        );
        String[] baseEffectKeys = {
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "trackerInstanceId",
                "trackerRole",
                "expectedContainerPosition",
                "slotIdentity",
                "slotSummary",
                "slotStackBefore",
                "slotStackAfter",
                "slotInPlayerInventory",
                "acceptPredicateEvaluated",
                "acceptPredicateResult",
                "predicateMatchReason",
                "predicateLastInteractionPresent",
                "predicateLastInteractionPosition",
                "observationOutcome",
                "beforeItem",
                "beforeCount",
                "afterItem",
                "afterCount",
                "deltaComponentCount",
                "deltaComponent1Item",
                "deltaComponent1Amount",
                "deltaComponent2Item",
                "deltaComponent2Amount"
        };
        assertNullStateFamilyKeys(baseEffect, baseEffectKeys);
        assertFalse(keys(baseEffect).contains("subscriptionGeneration"));
        assertEquals(0, field(baseEffect, "deltaComponentCount"));
        assertEquals("UNAVAILABLE", field(baseEffect, "deltaComponent1Item"));

        Object[] automaticSliceEffect = StoreDepositEventFields.effectObservationFields(
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                false,
                "UNAVAILABLE",
                false,
                null,
                "NO_CHANGE"
        );
        assertNullStateFamilyKeys(
                automaticSliceEffect,
                append(
                        baseEffectKeys,
                        "trackerIdentity",
                        "subscriptionGeneration",
                        "subscriptionActiveAtMutation",
                        "trackerTargetBinding",
                        "lastBlockPosInteractionAtEvent",
                        "predicateEvaluated",
                        "predicateResult",
                        "signedDelta"
                )
        );
        assertEquals("UNAVAILABLE", field(automaticSliceEffect, "subscriptionGeneration"));
        assertEquals("UNAVAILABLE", field(automaticSliceEffect, "trackerTargetBinding"));
        assertEquals("0,0", field(automaticSliceEffect, "signedDelta"));
    }

    @Test
    void terminalFamilyBuildersPreserveOrderedKeys() {
        assertNullStateFamilyKeys(
                StoreDepositEventFields.terminalSummaryFields(
                        null,
                        "ROOT_STOP_END",
                        "UNKNOWN_STOP",
                        null
                ),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "terminalTrigger",
                "durationMillis",
                "activationCount",
                "interruptCount",
                "resumeCount",
                "trueStopObserved",
                "naturalFinishObserved",
                "explicitStopCorrelated",
                "lifecycleEventCount",
                "childReconciliationCount",
                "parentCandidateDecisionCount",
                "filteredSearchResultCount",
                "pursuitDecisionCount",
                "targetCallbackDecisionCount",
                "craftRouteEventCount",
                "transferDecisionCount",
                "effectObservationCount",
                "expectedPositiveEffectCount",
                "exceptionObservationCount",
                "boundTaskCount",
                "droppedTaskBindingCount",
                "trackerBindingCount",
                "droppedTrackerBindingCount",
                "diagnosticBindingEvictionCount",
                "lastLifecycleAction",
                "lastLifecyclePhase",
                "finalActiveChildInstanceId",
                "finalActiveChildClass",
                "lifecycleCounts",
                "reconciliationOutcomeCountsByRole",
                "parentCandidateCounts",
                "filteredSearchCounts",
                "pursuitCounts",
                "targetCallbackCounts",
                "craftRouteCounts",
                "transferActionCounts",
                "effectObservationCounts",
                "exceptionCounts",
                "effectObservationComplete",
                "effectObservationCompletenessAuthority",
                "effectVerified",
                "diagnosticClassification",
                "coverageComplete"
        );

        assertNullStateFamilyKeys(
                StoreDepositEventFields.effectSummaryFields(null, "ROOT_STOP_END"),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "effectObservationCount",
                "expectedPositiveEffectCount",
                "effectObservationCounts",
                "effectObservationComplete",
                "effectObservationCompletenessAuthority",
                "effectVerified",
                "effectVerificationAuthority"
        );

        assertNullStateFamilyKeys(
                StoreDepositEventFields.baritoneSummaryFields(null, "ROOT_STOP_END"),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "baritoneContextImplemented",
                "baritoneContextCoverage",
                "coverageComplete"
        );

        assertNullStateFamilyKeys(
                StoreDepositEventFields.coverageSummaryFields(null, "ROOT_STOP_END", null),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "implementedFamilies",
                "unimplementedFamilies",
                "lifecycleEventCount",
                "childReconciliationCount",
                "parentCandidateDecisionCount",
                "filteredSearchResultCount",
                "pursuitDecisionCount",
                "targetCallbackDecisionCount",
                "craftRouteEventCount",
                "transferDecisionCount",
                "effectObservationCount",
                "exceptionObservationCount",
                "coverageComplete"
        );

        assertNullStateFamilyKeys(
                StoreDepositEventFields.terminalReserveExhaustedFields(
                        null,
                        "RESERVE_EXHAUSTED",
                        null
                ),
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "terminalGroupEmitted",
                "reservedTerminalSummarySlots",
                "coverageComplete"
        );
    }

    @Test
    void exceptionBuilderPreservesOrderedKeysAndNullRepresentations() {
        Object[] fields = StoreDepositEventFields.exceptionFields(
                null,
                null,
                null,
                "fixture-signature"
        );
        assertNullStateFamilyKeys(
                fields,
                "diagnosticScope",
                "owner",
                "mode",
                "trigger",
                "dedupe_key",
                "max_emission",
                "correlation",
                "payload",
                "terminal",
                "behavior_effect",
                "exceptionBoundary",
                "observedPosition",
                "exceptionSignature",
                "threadName"
        );
        assertEquals("unavailable", field(fields, "exceptionBoundary"));
        assertEquals("unavailable", field(fields, "observedPosition"));
        assertEquals("fixture-signature", field(fields, "exceptionSignature"));
    }

    @Test
    void mergePreservesFirstKeyPositionWhileSecondValueOverridesAndTailAppends() {
        Object[] first = {
                "alpha", 1,
                "overridden", 2,
                "nullable", null
        };
        Object[] second = {
                "overridden", 20,
                "omega", 3
        };

        assertArrayEquals(new Object[]{
                "alpha", 1,
                "overridden", 20,
                "nullable", null,
                "omega", 3
        }, StoreDepositEventFields.merge(first, second));
        assertArrayEquals(first, StoreDepositEventFields.merge(first, null));
        assertArrayEquals(second, StoreDepositEventFields.merge(null, second));
        assertArrayEquals(new Object[0], StoreDepositEventFields.merge(null, null));
        assertArrayEquals(
                new Object[]{"orphan", "key", 1},
                StoreDepositEventFields.merge(new Object[]{"orphan"}, new Object[]{"key", 1})
        );

        Object[] builderOverride = StoreDepositEventFields.parentCandidateDecisionFields(
                null,
                null,
                "ORIGINAL",
                null,
                false,
                false,
                null,
                null,
                new Object[]{
                        "selectedBranch", "OVERRIDDEN",
                        "extensionField", 42
                }
        );
        assertEquals("OVERRIDDEN", field(builderOverride, "selectedBranch"));
        assertEquals(42, field(builderOverride, "extensionField"));
        assertEquals("extensionField", keys(builderOverride).get(keys(builderOverride).size() - 1));

        Object[] budgetOverride = StoreDepositEventFields.coverageSummaryFields(
                null,
                "ROOT_STOP_END",
                new Object[]{
                        "coverageComplete", true,
                        "budgetTail", "present"
                }
        );
        assertEquals(true, field(budgetOverride, "coverageComplete"));
        assertEquals("budgetTail", keys(budgetOverride).get(keys(budgetOverride).size() - 1));
    }

    @Test
    void predicateSnapshotRemainsAPublicStaticRecordWithOrderedComponentsAndFactories() throws Exception {
        Class<StoreDepositEventFields.PredicateSnapshot> type =
                StoreDepositEventFields.PredicateSnapshot.class;
        assertTrue(type.isRecord());
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isStatic(type.getModifiers()));
        assertTrue(Modifier.isFinal(type.getModifiers()));

        RecordComponent[] components = type.getRecordComponents();
        assertEquals(3, components.length);
        assertEquals("matchReason", components[0].getName());
        assertEquals(String.class, components[0].getType());
        assertEquals("lastInteractionPresent", components[1].getName());
        assertEquals(boolean.class, components[1].getType());
        assertEquals("lastInteractionPosition", components[2].getName());
        assertEquals(BlockPos.class, components[2].getType());
        assertTrue(Modifier.isPublic(
                type.getDeclaredConstructor(String.class, boolean.class, BlockPos.class).getModifiers()
        ));

        Method unavailableFactory = type.getDeclaredMethod("unavailable");
        assertEquals(type, unavailableFactory.getReturnType());
        assertTrue(Modifier.isPublic(unavailableFactory.getModifiers()));
        assertTrue(Modifier.isStatic(unavailableFactory.getModifiers()));
        Method fromFactory = type.getDeclaredMethod(
                "from",
                Optional.class,
                BlockPos.class,
                boolean.class
        );
        assertEquals(type, fromFactory.getReturnType());
        assertTrue(Modifier.isPublic(fromFactory.getModifiers()));
        assertTrue(Modifier.isStatic(fromFactory.getModifiers()));

        assertEquals(
                new StoreDepositEventFields.PredicateSnapshot("UNAVAILABLE", false, null),
                StoreDepositEventFields.PredicateSnapshot.unavailable()
        );
        assertEquals(
                new StoreDepositEventFields.PredicateSnapshot(
                        "LAST_INTERACTION_ABSENT",
                        false,
                        null
                ),
                StoreDepositEventFields.PredicateSnapshot.from(null, new BlockPos(9, 9, 9), true)
        );
        assertEquals(
                new StoreDepositEventFields.PredicateSnapshot(
                        "LAST_INTERACTION_ABSENT",
                        false,
                        null
                ),
                StoreDepositEventFields.PredicateSnapshot.from(
                        Optional.empty(),
                        new BlockPos(9, 9, 9),
                        false
                )
        );

        BlockPos observed = new BlockPos(1, 64, 2);
        assertEquals(
                new StoreDepositEventFields.PredicateSnapshot("MATCH", true, observed),
                StoreDepositEventFields.PredicateSnapshot.from(
                        Optional.of(observed),
                        new BlockPos(8, 70, 8),
                        true
                )
        );
        assertEquals(
                new StoreDepositEventFields.PredicateSnapshot("POSITION_MISMATCH", true, observed),
                StoreDepositEventFields.PredicateSnapshot.from(
                        Optional.of(observed),
                        observed,
                        false
                )
        );

        Set<Class<?>> publicNestedTypes = new LinkedHashSet<>();
        for (Class<?> nested : StoreDepositEventFields.class.getDeclaredClasses()) {
            if (Modifier.isPublic(nested.getModifiers())) {
                publicNestedTypes.add(nested);
            }
        }
        assertEquals(Set.of(type), publicNestedTypes);
    }

    private static StoreDepositOperationState state(String requestSource) {
        return new StoreDepositOperationState(new StoreDepositOperationContext(
                "operation-fixture",
                requestSource,
                null,
                41L,
                7L
        ));
    }

    private static void assertNullStateFamilyKeys(Object[] fields, String... familyKeys) {
        List<String> expected = new ArrayList<>(UNAVAILABLE_OPERATION_KEYS);
        expected.addAll(Arrays.asList(familyKeys));
        assertEquals(expected, keys(fields));
    }

    private static void assertKeys(Object[] fields, String... expectedKeys) {
        assertEquals(Arrays.asList(expectedKeys), keys(fields));
    }

    private static List<String> keys(Object[] fields) {
        assertEquals(0, fields.length & 1, "Object[] fields must contain ordered key/value pairs");
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < fields.length; index += 2) {
            keys.add(String.valueOf(fields[index]));
        }
        return keys;
    }

    private static Object field(Object[] fields, String expectedKey) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (expectedKey.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + expectedKey);
    }

    private static String[] append(String[] first, String... second) {
        String[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private static MethodSignature signature(String name,
                                             Class<?> returnType,
                                             Class<?>... parameterTypes) {
        return new MethodSignature(
                name,
                returnType,
                List.copyOf(Arrays.asList(parameterTypes))
        );
    }

    private record MethodSignature(String name,
                                   Class<?> returnType,
                                   List<Class<?>> parameterTypes) {
    }
}
