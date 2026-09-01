package lavi.minecraft.diagnostics.crafting.acquisition.event;

import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticBoundedEventText;
import lavi.minecraft.diagnostics.session.admission.DiagnosticAdmissionDecision;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260901_kpopmodder: Lock event purpose, provenance, fingerprints, and payload bounds before emission.
class CraftResourceAcquisitionEventContractTest {
    private final CraftResourceAcquisitionEventContract contract =
            new CraftResourceAcquisitionEventContract();

    @Test
    void sourceEventVocabularyIsExactAndClosed() {
        assertEquals(Set.of(
                CraftResourceSourceEventName.VISIBLE_TASK_RETURN,
                CraftResourceSourceEventName.TASK_CHILD_RECONCILIATION,
                CraftResourceSourceEventName.SMELT_CHILD_SELECTION_STATE,
                CraftResourceSourceEventName.SMELT_MATERIAL_PROGRESS_SNAPSHOT,
                CraftResourceSourceEventName.FURNACE_OPERATION_GATE_TRANSITION,
                CraftResourceSourceEventName.FURNACE_CONTAINER_ROUTE_TRANSITION,
                CraftResourceSourceEventName.MINE_TARGET_SELECTION_TRANSITION,
                CraftResourceSourceEventName.MINE_TARGET_GOAL_REQUEST,
                CraftResourceSourceEventName.MINE_TARGET_ABANDONED,
                CraftResourceSourceEventName.CONTAINER_TASK_TARGET_DECISION,
                CraftResourceSourceEventName.CONTAINER_TASK_BRANCH,
                CraftResourceSourceEventName.CONTAINER_TASK_CHILD_RECONCILIATION,
                CraftResourceSourceEventName.CONTAINER_TASK_OWNER_STOP,
                CraftResourceSourceEventName.CRAFTING_TABLE_ROUTE_RETRY_SUMMARY,
                CraftResourceSourceEventName.CONTAINER_OPEN_ATTEMPT_OBSERVED,
                CraftResourceSourceEventName.CONTAINER_OPEN_RETURN_OBSERVED
        ), Set.copyOf(List.of(CraftResourceSourceEventName.values())));
    }

    @Test
    void unavailableSourceSequenceIsExplicitAndUnknownSourceNamesAreRejected() {
        Map<String, Object> reference = contract.sourceReference(
                "MINE_TARGET_GOAL_REQUEST",
                null
        );

        assertEquals(
                CraftResourceAcquisitionEventContract.UNAVAILABLE_SOURCE_EVENT_SEQUENCE,
                reference.get("sourceEventSequence")
        );
        assertThrows(IllegalArgumentException.class,
                () -> contract.sourceReference("INVENTED_SOURCE_EVENT", 1L));
    }

    @Test
    void exactEventNamesUseTheExistingSharedFamilyPrecedence() {
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL, contract.familyFor(
                "CRAFT_RESOURCE_REQUIREMENT_DECISION"));
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL, contract.familyFor(
                "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION"));
        assertEquals(DiagnosticEventFamily.EXCEPTION_COVERAGE, contract.familyFor(
                "CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH"));
        assertEquals(DiagnosticEventFamily.EXCEPTION_COVERAGE, contract.familyFor(
                "BLOCK_OPTIONAL_META_MANAGER_EXCEPTION"));
        assertEquals(DiagnosticEventFamily.EXCEPTION_COVERAGE, contract.familyFor(
                "BLOCK_OPTIONAL_META_MANAGER_COVERAGE_GAP"));
        assertEquals(DiagnosticEventFamily.ORDINARY_DETAIL, contract.familyFor(
                "BLOCK_OPTIONAL_META_MANAGER_FAILURE"));
        assertEquals(DiagnosticEventFamily.NON_STORE_TERMINAL, contract.familyFor(
                "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY"));
    }

    @Test
    void exceptionFamilyExhaustionCannotBorrowTerminalSlotsOrRetryAsAnotherFamily() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("craft-resource-family-test");
        for (int index = 0; index < DiagnosticEventFamily.EXCEPTION_COVERAGE.slotQuota(); index++) {
            assertTrue(authority.admit(contract.admissionRequest(
                    "CRAFT_RESOURCE_EXPECTED_OBSERVED_MISMATCH",
                    true
            )).admitted());
        }

        DiagnosticAdmissionDecision denied = authority.admit(contract.admissionRequest(
                "BLOCK_OPTIONAL_META_MANAGER_EXCEPTION",
                true
        ));
        DiagnosticAdmissionDecision terminal = authority.admit(contract.admissionRequest(
                "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY",
                true
        ));

        assertFalse(denied.admitted());
        assertEquals(
                DiagnosticAdmissionDecision.RejectionReason.FAMILY_QUOTA_EXHAUSTED,
                denied.rejectionReason()
        );
        assertTrue(terminal.admitted());
        assertEquals(DiagnosticEventFamily.NON_STORE_TERMINAL, terminal.token().family());
        assertEquals(16, authority.snapshot()
                .family(DiagnosticEventFamily.EXCEPTION_COVERAGE).admittedSlots());
        assertEquals(1, authority.snapshot()
                .family(DiagnosticEventFamily.NON_STORE_TERMINAL).admittedSlots());
    }

    @Test
    void sourceProjectionRetainsOnlyTheAuthoritativeEventReference() {
        Map<String, Object> reference = contract.sourceReference(
                "MINE_TARGET_SELECTION_TRANSITION",
                42L
        );

        assertEquals(Set.of("sourceEventName", "sourceEventSequence"), reference.keySet());
        assertEquals("MINE_TARGET_SELECTION_TRANSITION", reference.get("sourceEventName"));
        assertEquals(42L, reference.get("sourceEventSequence"));
        assertFalse(reference.containsKey("targetBlockId"));
        assertFalse(reference.containsKey("observedBlockState"));
        assertThrows(UnsupportedOperationException.class,
                () -> reference.put("duplicateAuthority", true));
    }

    @Test
    void semanticFingerprintExcludesIdentityTickTimestampAndOpaqueNoise() {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("event", "CRAFT_RESOURCE_TARGET_ROLE_TRANSITION");
        first.put("commandCorrelationId", "correlation-1");
        first.put("resourceStage", "IRON_INPUT_ACQUISITION");
        first.put("targetRole", "IRON_ORE_BLOCK");
        first.put("targetPosition", "-525,120,-1067");
        first.put("expectedBlockIds", List.of(
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore"));
        first.put("gameTick", 10L);
        first.put("timestamp", 100L);
        first.put("sourceTaskInstanceId", "task-a");
        first.put("randomId", "random-a");
        first.put("opaqueObjectString", "Object@1");

        Map<String, Object> second = new LinkedHashMap<>(first);
        second.put("commandCorrelationId", "different-opaque-correlation");
        second.put("expectedBlockIds", List.of(
                "minecraft:deepslate_iron_ore", "minecraft:iron_ore"));
        second.put("gameTick", 999L);
        second.put("timestamp", 999_999L);
        second.put("sourceTaskInstanceId", "task-b");
        second.put("randomId", "random-b");
        second.put("opaqueObjectString", "Object@2");

        assertEquals(contract.semanticFingerprint(first),
                contract.semanticFingerprint(second));

        second.put("targetPosition", "-524,120,-1067");
        assertFalse(contract.semanticFingerprint(first)
                .equals(contract.semanticFingerprint(second)));
    }

    @Test
    void physicalPayloadIsAlwaysBoundedToEightKiBUtf8WithExplicitPartialMarkers() {
        Map<String, Object> required = new LinkedHashMap<>();
        required.put("commandRequestId", "request-1");
        required.put("commandCorrelationId", "correlation-1");
        required.put("diagnosticCaptureStatus", "complete");
        required.put("sourceEventName", "MINE_TARGET_SELECTION_TRANSITION");
        required.put("sourceEventSequence", 42L);
        for (int index = 0; index < 60; index++) {
            required.put("requiredField" + index, "한".repeat(2_000));
        }
        Map<String, Object> optional = new LinkedHashMap<>();
        optional.put("boundedTargetHistory", "x".repeat(20_000));
        optional.put("stackFrame0", "y".repeat(20_000));

        DiagnosticBoundedEventText result = contract.format(
                "CRAFT_RESOURCE_ACQUISITION_TERMINAL_SUMMARY",
                required,
                optional
        );

        assertEquals(8_192, contract.physicalByteLimit());
        assertTrue(DiagnosticBoundedEventFormatter.utf8Length(result.text()) <= 8_192);
        assertTrue(result.partial());
        assertTrue(result.omittedOptionalFieldCount() > 0);
        assertTrue(result.text().contains("diagnosticCaptureStatus=partial"));
        assertTrue(result.text().contains("behavior_effect=none"));
        assertTrue(result.text().contains("sourceEventName="));
        assertTrue(result.text().contains("sourceEventSequence=42"));
    }
}
