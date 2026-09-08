package lavi.minecraft.diagnostics.container.store.deposit;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Record the exact 17-method and historical 16-group/24-assertion Slice A migration.
class StoreDepositSliceATestMigrationLedgerTest {
    private static final String TEST_ROOT = "src/test/java/";
    private static final String MIGRATION_LEDGER_RELATIVE_PATH =
            "lavi/minecraft/diagnostics/container/store/deposit/"
                    + "StoreDepositSliceATestMigrationLedgerTest.java";
    private static final String SUPPORT_RELATIVE_PATH =
            "lavi/minecraft/diagnostics/container/store/deposit/support/"
                    + "StoreDepositSliceATestSupport.java";
    private static final String REMOVED_UMBRELLA_RELATIVE_PATH =
            "lavi/minecraft/diagnostics/container/store/deposit/"
                    + "StoreDepositSliceADiagnosticsContractTest.java";
    private static final String TRANSFER_SELECTION = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/transfer/"
            + "StoreDepositTransferSelectionContractTest.java";
    private static final String SLOT_MUTATION = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/transfer/"
            + "StoreDepositSlotMutationContractTest.java";
    private static final String BINDING = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/binding/"
            + "StoreDepositBindingLifecycleContractTest.java";
    private static final String EFFECT = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/effect/"
            + "StoreDepositEffectProjectionContractTest.java";
    private static final String LIFECYCLE_ORDERING = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/lifecycle/"
            + "StoreDepositTaskLifecycleOrderingContractTest.java";
    private static final String MAINTENANCE_ORDERING = TEST_ROOT
            + "lavi/minecraft/task/container/deposit/auto/maintenance/"
            + "AutoDepositMaintenanceChildRegistrationOrderTest.java";
    private static final String PRESSURE_ORDERING = TEST_ROOT
            + "lavi/minecraft/task/container/deposit/auto/pressure/"
            + "AutoDepositPressureChainSourceContractTest.java";
    private static final String ROUTE = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/route/"
            + "StoreDepositRouteInvalidationContractTest.java";
    private static final String INTERACTION = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/interaction/"
            + "StoreDepositInteractionDiagnosticFieldsTest.java";
    private static final String CARRY_ON = TEST_ROOT
            + "lavi/minecraft/integration/carryon/container/"
            + "CarryOnContainerPickupEvidenceTest.java";
    private static final String AUTOMATIC_LEDGER = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/"
            + "StoreDepositAutomaticLifecycleLedgerContractTest.java";
    private static final String COVERAGE_GAP = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/"
            + "StoreDepositAutomaticCoverageGapIntegrationTest.java";
    private static final String BUDGET = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/budget/"
            + "StoreDepositDiagnosticsBoundednessContractTest.java";
    private static final String AUTOMATIC_BOUNDEDNESS = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/"
            + "StoreDepositAutomaticLifecycleLedgerBoundednessTest.java";
    private static final String CONTEXT_ISOLATION = TEST_ROOT
            + "lavi/minecraft/diagnostics/container/store/deposit/lifecycle/"
            + "StoreDepositAutomaticContextIsolationContractTest.java";
    private static final String POST_PLACE_HANDOFF = TEST_ROOT
            + "lavi/minecraft/task/container/deposit/handoff/"
            + "DepositAllPostPlaceHandoffSourceContractTest.java";

    private static final Map<String, List<Destination>> METHOD_DESTINATIONS = Map.ofEntries(
            entry("aggregateTargetAndSelectedPhysicalSourceHaveSeparateIdentityAndCount",
                    destination(TRANSFER_SELECTION,
                            "aggregateTargetAndSelectedPhysicalSourceHaveSeparateIdentityAndCount")),
            entry("partialPhysicalTransferKeepsLocalArithmeticSeparateFromDurability",
                    destination(TRANSFER_SELECTION,
                            "partialPhysicalTransferKeepsLocalArithmeticSeparateFromDurability")),
            entry("strictExactFitRejectsEqualityWhileDestinationIdentityParticipatesInChildEquality",
                    destination(TRANSFER_SELECTION,
                            "strictExactFitRejectsEqualityWhileDestinationIdentityParticipatesInChildEquality")),
            entry("oneSlotActionOwnsOrderedMutationIdentities",
                    destination(SLOT_MUTATION, "oneSlotActionOwnsOrderedMutationIdentities")),
            entry("bothTrackerRolesShareMutationIdentityAndLocalMutationLeavesDurabilityUnavailable",
                    destination(SLOT_MUTATION,
                            "bothTrackerRolesShareMutationIdentityAndLocalMutationLeavesDurabilityUnavailable")),
            entry("trackerResubscriptionAdvancesGenerationWithoutLosingActiveState",
                    destination(BINDING, "trackerResubscriptionAdvancesGenerationWithoutLosingActiveState")),
            entry("signedDeltasCoverPositiveNegativeReplacementAndTrackerPredicateAsymmetry",
                    destination(EFFECT,
                            "signedDeltasCoverPositiveNegativeReplacementAndTrackerPredicateAsymmetry")),
            entry("notStoredProjectionPreservesFullPartialAndZeroAvailableSemantics",
                    destination(TRANSFER_SELECTION,
                            "notStoredProjectionPreservesFullPartialAndZeroAvailableSemantics")),
            entry("availableCountCompositionUsesExistingCombinedValueWithoutDiagnosticRescan",
                    destination(TRANSFER_SELECTION,
                            "availableCountCompositionUsesExistingCombinedValueWithoutDiagnosticRescan")),
            Map.entry("parentBeforeChildTickOrderingRemainsUnchanged", List.of(
                    destination(LIFECYCLE_ORDERING, "parentReconciliationPrecedesChildTick"),
                    destination(MAINTENANCE_ORDERING, "maintenanceRegistersChildBeforeReturningIt"),
                    destination(PRESSURE_ORDERING, "pressureChainKeepsUserRootBindingShape")
            )),
            entry("checkFalseInvalidationDoesNotCloseRouteChildOrReconstructProgressProvenance",
                    destination(ROUTE,
                            "checkFalseInvalidationDoesNotCloseRouteChildOrReconstructProgressProvenance")),
            entry("headCaptureVerdictAndObservationLookupVerdictRemainTypedAndIndependent",
                    destination(INTERACTION,
                            "headCaptureVerdictAndObservationLookupVerdictRemainTypedAndIndependent")),
            entry("carryOnTemporalEdgeWithRetainedTargetAndUnavailableIdentityIsNotExactAttribution",
                    destination(CARRY_ON,
                            "carryOnTemporalEdgeWithRetainedTargetAndUnavailableIdentityIsNotExactAttribution")),
            Map.entry("automaticLifecycleScopesKeepSeparateExactCountsAndHandoffFlags", List.of(
                    destination(AUTOMATIC_LEDGER,
                            "automaticLifecycleScopesKeepSeparateExactCountsAndHandoffFlags"),
                    destination(AUTOMATIC_LEDGER, "sameUserRootRetainsOrderedAutomaticRuns"),
                    destination(AUTOMATIC_LEDGER, "missingPerItemCloseRemainsIncomplete"),
                    destination(AUTOMATIC_LEDGER, "wrongPerItemIdentitiesRemainIncomplete"),
                    destination(AUTOMATIC_LEDGER, "missingAndUnexpectedScopeIdentitiesRemainExplicit"),
                    destination(AUTOMATIC_LEDGER, "unavailableExpectedIdentityRecordsCoverageGap"),
                    destination(COVERAGE_GAP, "transferRegistryEvictionRemainsAnExplicitCoverageGap"),
                    destination(COVERAGE_GAP, "stagedTransferRegistryEvictionRemainsAnExplicitCoverageGap"),
                    destination(COVERAGE_GAP, "terminalExpectationRegistrationPrecedesObservations")
            )),
            Map.entry("diagnosticsModesAndAllBoundednessControlsRemainExplicit", List.of(
                    destination(BUDGET, "diagnosticsModesAndAllBoundednessControlsRemainExplicit"),
                    destination(BUDGET, "detailFamilyPolicyAndSessionCapsRemainBounded"),
                    destination(BUDGET, "criticalReserveAndGenericSuppressionCapsRemainBounded"),
                    destination(BUDGET, "payloadFormatterAndEmitterBoundariesRemainBounded"),
                    destination(AUTOMATIC_BOUNDEDNESS,
                            "automaticCoverageSuppressionSummaryRemainsBounded"),
                    destination(AUTOMATIC_BOUNDEDNESS, "terminalEmissionSuppressionRemainsExplicit"),
                    destination(AUTOMATIC_BOUNDEDNESS,
                            "activeRunAccessOrderLruAndPostRemovalSuppressionRemainExact")
            )),
            entry("sharedObserversNoOpWithoutAutomaticContextAndManualSourcesStayManual",
                    destination(CONTEXT_ISOLATION,
                            "sharedObserversNoOpWithoutAutomaticContextAndManualSourcesStayManual")),
            entry("automaticPostPlaceHandoffIsScopedToTheGeneralMaintenanceFactory",
                    destination(POST_PLACE_HANDOFF,
                            "automaticPostPlaceHandoffIsScopedToTheGeneralMaintenanceFactory"))
    );

    private static final List<HistoricalGroup> HISTORICAL_GROUPS = List.of(
            group(1, List.of(1), TRANSFER_SELECTION),
            group(2, List.of(2), TRANSFER_SELECTION),
            group(3, List.of(3, 4), TRANSFER_SELECTION),
            group(4, List.of(5), SLOT_MUTATION),
            group(5, List.of(6, 7), SLOT_MUTATION),
            group(6, List.of(8), BINDING),
            group(7, List.of(9, 10), EFFECT),
            group(8, List.of(11, 12, 13), TRANSFER_SELECTION),
            group(9, List.of(14), TRANSFER_SELECTION),
            group(10, List.of(15), LIFECYCLE_ORDERING, MAINTENANCE_ORDERING, PRESSURE_ORDERING),
            group(11, List.of(16, 17, 18), ROUTE),
            group(12, List.of(19), INTERACTION),
            group(13, List.of(20), CARRY_ON),
            group(14, List.of(21), AUTOMATIC_LEDGER, COVERAGE_GAP),
            group(15, List.of(22), BUDGET, AUTOMATIC_BOUNDEDNESS),
            group(16, List.of(23, 24), CONTEXT_ISOLATION)
    );

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void allSeventeenOriginalMethodsHaveExactDestinationTokens() throws IOException {
        Path runtimeTestJava = locateRuntimeTestJava();
        assertEquals(17, METHOD_DESTINATIONS.size());
        for (Map.Entry<String, List<Destination>> migration : METHOD_DESTINATIONS.entrySet()) {
            assertTrue(!migration.getValue().isEmpty(), migration.getKey());
            for (Destination destination : migration.getValue()) {
                String destinationSource = readDestination(runtimeTestJava, destination.path());
                assertTrue(destinationSource.contains(destination.requiredToken()),
                        migration.getKey() + " -> " + destination);
                assertTrue(destinationSource.contains("@BeforeEach"), destination.path());
                assertTrue(destinationSource.contains("@AfterEach"), destination.path());
            }
        }
    }

    @Test
    void historicalSixteenGroupsCoverAssertionsOneThroughTwentyFourExactlyOnce() throws IOException {
        Path runtimeTestJava = locateRuntimeTestJava();
        assertEquals(16, HISTORICAL_GROUPS.size());
        assertEquals(
                IntStream.rangeClosed(1, 16).boxed().toList(),
                HISTORICAL_GROUPS.stream().map(HistoricalGroup::scenario).toList()
        );
        assertEquals(
                IntStream.rangeClosed(1, 24).boxed().toList(),
                HISTORICAL_GROUPS.stream().flatMap(group -> group.assertions().stream()).toList()
        );
        for (HistoricalGroup group : HISTORICAL_GROUPS) {
            for (String owner : group.owners()) {
                assertTrue(readDestination(runtimeTestJava, owner)
                                .contains("scenario " + group.scenario() + " [assertion"),
                        "scenario " + group.scenario() + " -> " + owner);
            }
        }
    }

    @Test
    void removedUmbrellaTestCannotBeReintroducedAlongsideItsOwners() throws IOException {
        Path removedUmbrella = locateRuntimeTestJava().resolve(REMOVED_UMBRELLA_RELATIVE_PATH);
        assertFalse(Files.exists(removedUmbrella));
    }

    private static Map.Entry<String, List<Destination>> entry(String method,
                                                               Destination destination) {
        return Map.entry(method, List.of(destination));
    }

    private static Destination destination(String path, String requiredToken) {
        return new Destination(path, requiredToken);
    }

    private static HistoricalGroup group(int scenario,
                                         List<Integer> assertions,
                                         String... owners) {
        return new HistoricalGroup(scenario, assertions, List.of(owners));
    }

    private static Path locateRuntimeTestJava() throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            for (String relativeRoot : List.of(
                    "src/test/java",
                    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/java"
            )) {
                Path candidate = current.resolve(relativeRoot);
                if (Files.isRegularFile(candidate.resolve(MIGRATION_LEDGER_RELATIVE_PATH))
                        && Files.isRegularFile(candidate.resolve(SUPPORT_RELATIVE_PATH))) {
                    return candidate;
                }
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate exact Fabric 1.20.1 Slice A test source root");
    }

    private static String readDestination(Path runtimeTestJava, String sourcePath) throws IOException {
        if (!sourcePath.startsWith(TEST_ROOT)) {
            throw new IOException("Destination is outside the runtime test source root: " + sourcePath);
        }
        return Files.readString(runtimeTestJava.resolve(sourcePath.substring(TEST_ROOT.length())))
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private record Destination(String path, String requiredToken) {
    }

    private record HistoricalGroup(int scenario,
                                   List<Integer> assertions,
                                   List<String> owners) {
    }
}
