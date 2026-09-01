package lavi.minecraft.diagnostics.crafting.acquisition.target;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Define semantic target-attempt accounting without Task identity noise.
class CraftResourceTargetAttemptLedgerTest {
    @Test
    void candidateAllocationAndEqualReconciliationDoNotStartAnAttempt() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple tuple = ironOre("-525,120,-1067");

        CraftResourceTargetAttemptDecision candidate = ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.CANDIDATE_RETURN,
                tuple
        ));
        CraftResourceTargetAttemptDecision reconciled = ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.EQUAL_RECONCILIATION,
                tuple
        ));

        assertFalse(candidate.startedNewAttempt());
        assertFalse(reconciled.startedNewAttempt());
        assertEquals(OptionalLong.empty(), candidate.targetAttemptSequence());
        assertEquals(OptionalLong.empty(), reconciled.targetAttemptSequence());
        assertEquals(0L, ledger.snapshot().attemptTransitionCount());
        assertTrue(ledger.snapshot().currentTuple().isEmpty());
    }

    @Test
    void firstActiveTupleStartsOnceAndSemanticRepeatsKeepTheSequence() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple firstOrdering = new CraftResourceTargetTuple(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                "-525,120,-1067",
                List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")
        );
        CraftResourceTargetTuple reversedOrdering = new CraftResourceTargetTuple(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                "-525,120,-1067",
                List.of("minecraft:deepslate_iron_ore", "minecraft:iron_ore")
        );

        CraftResourceTargetAttemptDecision first = ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN,
                firstOrdering
        ));
        List<CraftResourceTargetObservationKind> unchangedKinds = List.of(
                CraftResourceTargetObservationKind.GOAL_SUBMISSION,
                CraftResourceTargetObservationKind.CANDIDATE_RETURN,
                CraftResourceTargetObservationKind.EQUAL_RECONCILIATION,
                CraftResourceTargetObservationKind.DETAIL_SUPPRESSED,
                CraftResourceTargetObservationKind.ADMISSION_DENIED
        );
        for (CraftResourceTargetObservationKind kind : unchangedKinds) {
            CraftResourceTargetAttemptDecision repeat = ledger.observe(observation(
                    CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                    kind,
                    reversedOrdering
            ));
            assertFalse(repeat.startedNewAttempt(), kind.name());
            assertEquals(OptionalLong.of(1L), repeat.targetAttemptSequence(), kind.name());
        }

        assertTrue(first.startedNewAttempt());
        assertEquals(OptionalLong.of(1L), first.targetAttemptSequence());
        assertEquals(1L, ledger.snapshot().targetAttemptSequence());
        assertEquals(1L, ledger.snapshot().attemptTransitionCount());
        assertEquals(firstOrdering, reversedOrdering);
    }

    @Test
    void differentTupleOrAuthoritativeReselectionStartsExactlyOneNextAttempt() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple first = ironOre("-525,120,-1067");
        CraftResourceTargetTuple second = ironOre("-524,120,-1067");
        assertEquals(OptionalLong.of(1L), active(ledger, first).targetAttemptSequence());

        CraftResourceTargetAttemptDecision chainSwitch = ledger.observe(new CraftResourceTargetObservation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.CHAIN_SWITCH,
                Optional.empty()
        ));
        assertFalse(chainSwitch.startedNewAttempt());
        assertEquals(OptionalLong.of(1L), chainSwitch.targetAttemptSequence());

        CraftResourceTargetAttemptDecision changed = active(ledger, second);
        assertTrue(changed.startedNewAttempt());
        assertEquals(OptionalLong.of(2L), changed.targetAttemptSequence());

        CraftResourceTargetAttemptDecision abandoned = ledger.observe(new CraftResourceTargetObservation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.TARGET_ABANDONED,
                Optional.empty()
        ));
        assertFalse(abandoned.startedNewAttempt());
        assertTrue(ledger.snapshot().currentTuple().isEmpty());

        CraftResourceTargetAttemptDecision reselected = active(ledger, second);
        assertTrue(reselected.startedNewAttempt());
        assertEquals(OptionalLong.of(3L), reselected.targetAttemptSequence());
        assertEquals(3L, ledger.snapshot().attemptTransitionCount());
    }

    @Test
    void terminalClosureRetainsTheLastOwnedTupleWithoutStartingAnotherAttempt() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple tuple = ironOre("-525,120,-1067");
        active(ledger, tuple);

        CraftResourceTargetAttemptDecision closed = ledger.observe(
                new CraftResourceTargetObservation(
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        CraftResourceTargetObservationKind.COMMAND_TERMINAL,
                        Optional.of(tuple)
                )
        );

        CraftResourceTargetAttemptSnapshot snapshot = ledger.snapshot();
        assertFalse(closed.startedNewAttempt());
        assertEquals(OptionalLong.of(1L), closed.targetAttemptSequence());
        assertTrue(snapshot.currentTuple().isEmpty());
        assertTrue(snapshot.lastClosure().isPresent());
        assertEquals(1L, snapshot.lastClosure().get().targetAttemptSequence());
        assertEquals(tuple, snapshot.lastClosure().get().targetTuple());
        assertEquals(
                CraftResourceTargetObservationKind.COMMAND_TERMINAL,
                snapshot.lastClosure().get().closureKind()
        );
        assertEquals(1L, snapshot.attemptTransitionCount());
    }

    @Test
    void unownedAndUnknownObservationsNeverReceiveOrAdvanceAnAttemptSequence() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple tuple = ironOre("-525,120,-1067");

        CraftResourceTargetAttemptDecision unowned = ledger.observe(observation(
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN,
                tuple
        ));
        CraftResourceTargetAttemptDecision unknown = ledger.observe(observation(
                CraftResourceAssociationStatus.UNKNOWN,
                CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN,
                tuple
        ));

        assertEquals(OptionalLong.empty(), unowned.targetAttemptSequence());
        assertEquals(OptionalLong.empty(), unknown.targetAttemptSequence());
        assertFalse(unowned.startedNewAttempt());
        assertFalse(unknown.startedNewAttempt());
        assertEquals(0L, ledger.snapshot().targetAttemptSequence());
        assertEquals(1L, ledger.snapshot().unownedObservationCount());
        assertEquals(1L, ledger.snapshot().unknownAssociationCount());
    }

    @Test
    void detailEligibilityAndHistoryStayBoundedWhileAggregateSequenceContinues() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        int detailEligible = 0;
        for (int sequence = 1; sequence <= 70; sequence++) {
            CraftResourceTargetAttemptDecision decision = active(
                    ledger,
                    ironOre(sequence + ",64,0")
            );
            if (decision.detailEligible()) {
                detailEligible++;
            }
        }

        CraftResourceTargetAttemptSnapshot snapshot = ledger.snapshot();
        assertEquals(70L, snapshot.targetAttemptSequence());
        assertEquals(70L, snapshot.attemptTransitionCount());
        assertEquals(64, detailEligible);
        assertEquals(64L, snapshot.detailEligibleTransitionCount());
        assertEquals(8, snapshot.history().size());
        assertEquals(List.of(1L, 2L, 3L, 4L, 67L, 68L, 69L, 70L),
                snapshot.history().stream()
                        .map(CraftResourceTargetHistorySample::targetAttemptSequence)
                        .toList());
        assertEquals(62L, snapshot.omittedTargetSampleCount());
    }

    @Test
    void onlyOwnedFailureSignalsUpdateUnreachableAndBlacklistCounters() {
        CraftResourceTargetAttemptLedger ledger = new CraftResourceTargetAttemptLedger();
        CraftResourceTargetTuple tuple = ironOre("-525,120,-1067");
        active(ledger, tuple);

        ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.UNREACHABLE_REQUEST,
                tuple
        ));
        ledger.observe(observation(
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                CraftResourceTargetObservationKind.UNREACHABLE_REQUEST,
                tuple
        ));
        ledger.observe(observation(
                CraftResourceAssociationStatus.UNKNOWN,
                CraftResourceTargetObservationKind.UNREACHABLE_REQUEST,
                tuple
        ));
        ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.BLACKLIST_STATE_CHANGED,
                tuple
        ));
        ledger.observe(observation(
                CraftResourceAssociationStatus.CONCURRENT_CHAIN_UNOWNED,
                CraftResourceTargetObservationKind.BLACKLIST_STATE_CHANGED,
                tuple
        ));

        CraftResourceTargetAttemptSnapshot snapshot = ledger.snapshot();
        assertEquals(1L, snapshot.unreachableRequestCount());
        assertEquals(1L, snapshot.blacklistTransitionCount());
        assertEquals(2L, snapshot.unownedObservationCount());
        assertEquals(1L, snapshot.unknownAssociationCount());
        assertEquals(1L, snapshot.targetAttemptSequence());
    }

    @Test
    void resourceStageAndTargetRoleRemainIndependentFixedVocabularies() {
        Set<String> stages = Arrays.stream(CraftResourceStage.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        Set<String> roles = Arrays.stream(CraftResourceTargetRole.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "RECIPE_PLANNING",
                "IRON_INPUT_ACQUISITION",
                "RAW_IRON_SMELTING",
                "FINAL_CRAFTING",
                "PLACEMENT_SUPPORT",
                "UNKNOWN"
        ), stages);
        assertEquals(Set.of(
                "IRON_ORE_BLOCK",
                "RAW_IRON_DROP",
                "FURNACE_ACQUISITION",
                "FURNACE_INTERACTION",
                "CRAFTING_TABLE_ACQUISITION",
                "CRAFTING_TABLE_INTERACTION",
                "PLACEMENT_SUPPORT_BLOCK",
                "UNKNOWN"
        ), roles);
        assertTrue(stages.stream().noneMatch(value -> value.contains("_OR_")));
        assertTrue(roles.stream().noneMatch(value -> value.contains("_OR_")));
    }

    private static CraftResourceTargetAttemptDecision active(
            CraftResourceTargetAttemptLedger ledger,
            CraftResourceTargetTuple tuple) {
        return ledger.observe(observation(
                CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN,
                tuple
        ));
    }

    private static CraftResourceTargetObservation observation(
            CraftResourceAssociationStatus association,
            CraftResourceTargetObservationKind kind,
            CraftResourceTargetTuple tuple) {
        return new CraftResourceTargetObservation(association, kind, Optional.of(tuple));
    }

    private static CraftResourceTargetTuple ironOre(String position) {
        return new CraftResourceTargetTuple(
                CraftResourceStage.IRON_INPUT_ACQUISITION,
                CraftResourceTargetRole.IRON_ORE_BLOCK,
                position,
                List.of("minecraft:iron_ore", "minecraft:deepslate_iron_ore")
        );
    }
}
