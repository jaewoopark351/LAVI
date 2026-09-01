package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenancePhase;
import lavi.minecraft.task.container.deposit.auto.maintenance.AutoDepositMaintenanceTask;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyItemSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicySnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import lavi.minecraft.testsupport.TestItems;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositPlanBuilderTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void excludesAnEntireItemTypeWhenProtectedAndPlainStacksAreMixed() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Item chestplate = TestItems.item();
        AutoDepositStackSnapshot selected = AutoDepositPolicyTestFixtures.stack(
                chestplate, "minecraft:iron_chestplate", 1, 0, true, false, false,
                AutoDepositItemRole.CHESTPLATE, 500
        );
        AutoDepositStackSnapshot plain = AutoDepositPolicyTestFixtures.stack(
                chestplate, "minecraft:iron_chestplate", 1, 1, false, false, false,
                AutoDepositItemRole.CHESTPLATE, 400
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositPlanBuilder builder = builder(definition);
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(List.of(selected, plain));

        AutoDepositPlanDraft draft = builder.prepare(
                idleContext(), List.of(selected, plain), hard, Map.of(), 2, 1
        );
        AutoDepositPlan plan = builder.finish(draft, Optional.empty(), 1L, "none");

        assertFalse(plan.hasTargets());
        assertEquals(AutoDepositDisposition.HARD_PROTECTED, plan.dispositions().get(chestplate));

        AutoDepositPolicyItemSnapshot decision = plan.policyItemDecisions().get(0);
        assertEquals(2, decision.physicalStacks().size());
        assertTrue(decision.physicalStacks().get(0).hardProtected());
        assertFalse(decision.physicalStacks().get(1).hardProtected());
        assertEquals("NOT_EVALUATED_HARD_PROTECTION", decision.classificationConfidence());
        assertEquals("NOT_EVALUATED_HARD_PROTECTION", decision.classificationProvenance());
        assertTrue(decision.observationComplete());

        AutoDepositPlan sameInventoryDifferentTrustedRevision = builder.finish(
                draft, Optional.empty(), 2L, "none"
        );
        StoreDepositAutomaticContext automaticContext = new StoreDepositAutomaticContext(
                true,
                12L,
                1L,
                "auto-deposit-12",
                "auto-deposit-12-maintenance-1",
                "auto-deposit-12-child-1",
                0,
                "auto-deposit-12-pressure-run-1"
        );
        DepositAllInventoryPressureSnapshot pressure =
                new DepositAllInventoryPressureSnapshot(2, 36);
        AutoDepositPolicySnapshot firstSnapshot = AutoDepositPolicySnapshot.capture(
                plan, pressure, "READY", "ready", automaticContext
        );
        AutoDepositPolicySnapshot secondSnapshot = AutoDepositPolicySnapshot.capture(
                sameInventoryDifferentTrustedRevision,
                pressure,
                "READY",
                "ready",
                automaticContext
        );
        assertEquals(firstSnapshot.inventorySnapshotId(), secondSnapshot.inventorySnapshotId());
        assertNotEquals(firstSnapshot.autoPlanId(), secondSnapshot.autoPlanId());
        assertEquals("auto-deposit-12", firstSnapshot.automaticContext().autoOperationId());
        assertEquals(2, firstSnapshot.itemDecisions().get(0).physicalStacks().size());
        assertTrue(firstSnapshot.observationComplete());

        AutoDepositPlanningResult diagnosticFailure = AutoDepositPlanningResult.failedAfterPlan(
                AutoDepositPlanningResult.Status.CONTEXT_CHANGED,
                "context_changed_after_plan",
                plan.fingerprint(),
                plan
        );
        assertTrue(diagnosticFailure.plan().isEmpty());
        assertEquals(plan, diagnosticFailure.diagnosticPlan().orElseThrow());
    }

    @Test
    void usesMaxOfWorkingSetAndCategoryReserveInsteadOfAddingThem() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Item logs = TestItems.item();
        AutoDepositStackSnapshot surplusStack = AutoDepositPolicyTestFixtures.stack(
                logs, "minecraft:oak_log", 64, 0, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot reservedStack = AutoDepositPolicyTestFixtures.stack(
                logs, "minecraft:oak_log", 16, 1, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        List<AutoDepositStackSnapshot> stacks = List.of(surplusStack, reservedStack);
        Object world = new Object();
        TestTask root = new TestTask();
        WorkingSetSnapshot workingSet = new WorkingSetSnapshot(
                root,
                List.of(root),
                world,
                Dimension.OVERWORLD,
                7L,
                Map.of(logs, 80),
                Map.of(logs, 80),
                Map.of(logs, 16)
        );
        AutoDepositContextSnapshot context = new AutoDepositContextSnapshot(
                world,
                Dimension.OVERWORLD,
                "test-world",
                7L,
                root,
                workingSet,
                List.of("root")
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        Map<Item, Integer> reserves = new AutoDepositCategoryReservePolicy(
                definition, new AutoDepositItemClassificationPolicy(definition)
        ).allocate(stacks, hard, false);

        AutoDepositPlan plan = builder(definition).finish(
                builder(definition).prepare(context, stacks, hard, reserves, 2, 1),
                Optional.empty(),
                1L,
                "none"
        );

        assertEquals(Integer.valueOf(16), plan.protectedCounts().get(logs));
        assertEquals(64, plan.generalTargets()[0].getTargetCount());
        AutoDepositPolicyItemSnapshot decision = plan.policyItemDecisions().get(0);
        assertFalse(decision.observationComplete());
        assertTrue(decision.missingBoundaries().contains("WORKING_SET_RESERVATION_PROVENANCE"));
        assertTrue(decision.missingBoundaries().contains("CATEGORY_RESERVE_REASON"));
        assertTrue(decision.missingBoundaries().contains("CLASSIFICATION_CONFIDENCE"));
        assertTrue(decision.missingBoundaries().contains("CLASSIFICATION_PROVENANCE"));
    }

    @Test
    void omitsDiagnosticPolicyPayloadWhenDiagnosticsAreOff() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        Item logs = TestItems.item();
        AutoDepositStackSnapshot stack = AutoDepositPolicyTestFixtures.stack(
                logs, "minecraft:oak_log", 64, 0, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(List.of(stack));
        AutoDepositPlanBuilder builder = builder(definition);

        AutoDepositPlan plan = builder.finish(
                builder.prepare(idleContext(), List.of(stack), hard, Map.of(), 1, 1),
                Optional.empty(),
                0L,
                "not_required"
        );

        assertTrue(plan.hasTargets());
        assertTrue(plan.policyItemDecisions().isEmpty());
        assertEquals("UNAVAILABLE_DIAGNOSTICS_OFF", plan.diagnosticInventoryFingerprint());
        AutoDepositPolicySnapshot lateSnapshot = AutoDepositPolicySnapshot.capture(
                plan,
                new DepositAllInventoryPressureSnapshot(1, 36),
                "READY",
                "late_diagnostics_enable",
                StoreDepositAutomaticContext.unavailable()
        );
        assertFalse(lateSnapshot.observationComplete());
        assertEquals(
                "POLICY_DECISIONS_NOT_RETAINED_DIAGNOSTICS_OFF",
                lateSnapshot.missingBoundaries()
        );
    }

    @Test
    void retainsNonMainOnlyPhysicalFactsAsExplicitDiagnosticExclusions() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Item shield = TestItems.item();
        AutoDepositStackSnapshot offhand = AutoDepositPolicyTestFixtures.stackAt(
                shield,
                "minecraft:shield",
                1,
                40,
                AutoDepositStackLocation.OFFHAND,
                false,
                false,
                false,
                AutoDepositItemRole.NONE,
                0
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(List.of(offhand));
        AutoDepositPlanBuilder builder = builder(definition);

        AutoDepositPlan plan = builder.finish(
                builder.prepare(idleContext(), List.of(offhand), hard, Map.of(), 1, 1),
                Optional.empty(),
                0L,
                "not_required"
        );
        AutoDepositPolicySnapshot snapshot = AutoDepositPolicySnapshot.capture(
                plan,
                new DepositAllInventoryPressureSnapshot(1, 36),
                "READY",
                "non_main_only",
                StoreDepositAutomaticContext.unavailable()
        );

        assertFalse(plan.hasTargets());
        assertEquals(1, plan.policyItemDecisions().size());
        AutoDepositPolicyItemSnapshot decision = plan.policyItemDecisions().get(0);
        assertEquals("EXCLUDED", decision.decision());
        assertEquals("EXCLUDED_NON_MAIN_INVENTORY", decision.decisionReason());
        assertEquals(1, decision.physicalStacks().size());
        assertEquals("OFFHAND", decision.physicalStacks().get(0).location());
        assertTrue(decision.observationComplete());
        assertTrue(snapshot.observationComplete());
        assertEquals(1, snapshot.itemDecisions().stream()
                .mapToInt(item -> item.physicalStacks().size())
                .sum());
    }

    @Test
    void routesValuablesOnlyToTrustedDestinationAndFailsClosedForUnknownItems() {
        Item diamond = TestItems.item();
        Item unknown = TestItems.item();
        AutoDepositStackSnapshot diamondStack = AutoDepositPolicyTestFixtures.stack(
                diamond, "minecraft:diamond", 64, 0, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositStackSnapshot unknownStack = AutoDepositPolicyTestFixtures.stack(
                unknown, "examplemod:unknown_part", 64, 1, false, false, false,
                AutoDepositItemRole.NONE, 0
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositPlanBuilder builder = builder(definition);
        List<AutoDepositStackSnapshot> stacks = List.of(diamondStack, unknownStack);
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder.prepare(idleContext(), stacks, hard, Map.of(), 2, 1);

        AutoDepositPlan withoutTrust = builder.finish(draft, Optional.empty(), 1L, "none");
        AutoDepositTrustedDestinationCandidate candidate =
                new AutoDepositTrustedDestinationCandidate(
                        new AutoDepositTrustedDestination(
                                "test-world",
                                Dimension.OVERWORLD,
                                new BlockPos(10, 64, 10),
                                true
                        ),
                        0,
                        100.0,
                        "capacity_unverified"
                );
        AutoDepositPlan withTrust = builder.finish(
                draft, List.of(candidate), 2L, "capacity_unverified"
        );

        assertFalse(withoutTrust.hasTargets());
        assertEquals(1, withTrust.trustedTargets().length);
        assertEquals(64, withTrust.trustedTargets()[0].getTargetCount());
        assertEquals(AutoDepositDisposition.UNCLASSIFIED_CONSERVATIVE,
                withTrust.dispositions().get(unknown));
        assertTrue(withTrust.trustedDestination().isPresent());
        assertEquals(1, withTrust.trustedCandidates().size());
        assertEquals(candidate.destinationId(),
                withTrust.trustedCandidates().get(0).destinationId());
    }

    @Test
    void splitsRepeatedItemsIntoSingleStackStepsAndStopsAtTheReliefGoal() {
        Item ingot = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 32, 2, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 3, 2
        );

        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.empty(), 1L, "none"
        );

        assertEquals(2, plan.generalTargets().length);
        assertEquals(64, plan.generalTargets()[0].getTargetCount());
        assertEquals(64, plan.generalTargets()[1].getTargetCount());
        assertEquals(2, plan.expectedFreedSlots());
        assertEquals(2, plan.targetReliefSlots());
    }

    @Test
    void usesGeneralSurplusBeforeMovingAValuableToTrustedStorage() {
        Item ingot = TestItems.item();
        Item diamond = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        diamond, "minecraft:diamond", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 2, 1
        );

        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.of(new BlockPos(10, 64, 10)), 2L, "eligible"
        );

        assertEquals(1, plan.generalTargets().length);
        assertEquals(0, plan.trustedTargets().length);
        assertTrue(plan.trustedDestination().isEmpty());
    }

    @Test
    void reservesTrustedCapacityBeforeExecutingGeneralStepsWhenBothAreNeeded() {
        Item ingot = TestItems.item();
        Item diamond = TestItems.item();
        List<AutoDepositStackSnapshot> stacks = List.of(
                AutoDepositPolicyTestFixtures.stack(
                        ingot, "minecraft:iron_ingot", 64, 0, false, false, false,
                        AutoDepositItemRole.NONE, 0
                ),
                AutoDepositPolicyTestFixtures.stack(
                        diamond, "minecraft:diamond", 64, 1, false, false, false,
                        AutoDepositItemRole.NONE, 0
                )
        );
        AutoDepositPolicyDefinition definition = AutoDepositPolicyTestFixtures.definition();
        AutoDepositHardProtectionResult hard =
                new AutoDepositHardProtectionPolicy(definition).evaluate(stacks);
        AutoDepositPlanDraft draft = builder(definition).prepare(
                idleContext(), stacks, hard, Map.of(), 2, 2
        );
        AutoDepositPlan plan = builder(definition).finish(
                draft, Optional.of(new BlockPos(10, 64, 10)), 2L, "eligible"
        );

        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        plan.trustedCandidates().forEach(candidate ->
                repository.register(candidate.destination()));
        AutoDepositMaintenanceTask maintenance = new AutoDepositMaintenanceTask(plan, repository);

        assertEquals(1, plan.generalTargets().length);
        assertEquals(1, plan.trustedTargets().length);
        assertEquals(AutoDepositMaintenancePhase.DEPOSIT_TRUSTED, maintenance.phase());
    }

    private static AutoDepositPlanBuilder builder(AutoDepositPolicyDefinition definition) {
        return new AutoDepositPlanBuilder(
                definition, new AutoDepositItemClassificationPolicy(definition)
        );
    }

    private static AutoDepositContextSnapshot idleContext() {
        return new AutoDepositContextSnapshot(
                new Object(),
                Dimension.OVERWORLD,
                "test-world",
                1L,
                null,
                null,
                List.of("idle")
        );
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "auto-deposit policy test task";
        }
    }
}
