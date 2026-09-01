#20260901_kpopmodder: Guard the diagnostics-only iron-pickaxe acquisition boundary.
import re
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "runtime"
    / "chatclef_fabric_1.20.1"
    / "src"
    / "main"
    / "java"
)
ACQUISITION_ROOT = (
    JAVA_ROOT
    / "lavi"
    / "minecraft"
    / "diagnostics"
    / "crafting"
    / "acquisition"
)
FABRIC_PROJECTION_ROOT = (
    JAVA_ROOT
    / "lavi"
    / "minecraft"
    / "fabric"
    / "chatclef"
    / "bridge"
    / "command"
    / "diagnostics"
    / "crafting"
)


class MinecraftFabricChatClefIronPickaxeDiagnosticsContractTests(unittest.TestCase):
    def _acquisition_java_files(self):
        self.assertTrue(ACQUISITION_ROOT.is_dir(), str(ACQUISITION_ROOT))
        files = sorted(ACQUISITION_ROOT.rglob("*.java"))
        self.assertTrue(files, "the acquisition diagnostics package must contain Java sources")
        return files

    def _acquisition_text(self):
        return "\n".join(
            path.read_text(encoding="utf-8")
            for path in self._acquisition_java_files()
        )

    def _projection_text(self):
        self.assertTrue(FABRIC_PROJECTION_ROOT.is_dir(), str(FABRIC_PROJECTION_ROOT))
        files = sorted(FABRIC_PROJECTION_ROOT.rglob("*.java"))
        self.assertTrue(files, "the Fabric acquisition projection must contain Java sources")
        return "\n".join(path.read_text(encoding="utf-8") for path in files)

    def test_acquisition_diagnostics_are_split_into_five_responsibility_packages(self):
        responsibilities = (
            "scope",
            "association",
            "requirement",
            "target",
            "terminal",
        )

        self.assertEqual([], list(ACQUISITION_ROOT.glob("*.java")))
        for responsibility in responsibilities:
            package_dir = ACQUISITION_ROOT / responsibility
            self.assertTrue(package_dir.is_dir(), str(package_dir))
            self.assertTrue(
                list(package_dir.glob("*.java")),
                f"{responsibility} must own at least one direct Java type",
            )

    def test_every_acquisition_java_file_has_one_matching_top_level_type(self):
        declaration = re.compile(
            r"(?m)^(?:public\s+)?"
            r"(?:(?:abstract|final|sealed|non-sealed)\s+)*"
            r"(?:class|interface|enum|record)\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"
        )

        for path in self._acquisition_java_files():
            text = path.read_text(encoding="utf-8")
            relative_package = path.parent.relative_to(JAVA_ROOT)
            expected_package = ".".join(relative_package.parts)
            declarations = declaration.findall(text)

            self.assertIn(f"package {expected_package};", text, str(path))
            self.assertEqual([path.stem], declarations, str(path))

    def test_projections_reference_existing_authoritative_event_names(self):
        text = self._acquisition_text()
        authoritative_source_events = (
            "VISIBLE_TASK_RETURN",
            "TASK_CHILD_RECONCILIATION",
            "SMELT_CHILD_SELECTION_STATE",
            "SMELT_MATERIAL_PROGRESS_SNAPSHOT",
            "FURNACE_OPERATION_GATE_TRANSITION",
            "FURNACE_CONTAINER_ROUTE_TRANSITION",
            "MINE_TARGET_SELECTION_TRANSITION",
            "MINE_TARGET_GOAL_REQUEST",
            "CONTAINER_TASK_TARGET_DECISION",
            "CRAFTING_TABLE_ROUTE_RETRY_SUMMARY",
            "CONTAINER_OPEN_ATTEMPT_OBSERVED",
            "CONTAINER_OPEN_RETURN_OBSERVED",
        )

        self.assertIn("sourceEventName", text)
        self.assertIn("sourceEventSequence", text)
        for event_name in authoritative_source_events:
            self.assertIn(event_name, text, event_name)

    def test_acquisition_package_cannot_call_behavior_or_wire_boundaries(self):
        text = self._acquisition_text()
        forbidden_patterns = {
            "TaskRunner ownership": r"\bTaskRunner\b",
            "Task lifecycle inheritance": r"\bextends\s+Task\b",
            "Task scheduling": r"\b(?:runUserTask|setTask|replaceTask)\s*\(",
            "input mutation": r"\bsetInputForceState\s*\(",
            "input override ownership": r"\bInputOverrideHandler\b",
            "path mutation": r"\b(?:setGoalAndPath|cancelPath|cancelEverything)\s*\(",
            "path ownership": r"\b(?:CustomGoalProcess|PathingBehavior)\b",
            "retry behavior": r"(?i)\b(?:retry|scheduleRetry|incrementRetry)\s*\(",
            "cancellation behavior": r"(?i)\b(?:cancel|forceCancel)\s*\(",
            "result send behavior": r"(?i)\b(?:send|sendResult|sendEnvelope)\s*\(",
            "wire protocol import": r"lavi\.minecraft\.fabric\.chatclef\.bridge\.protocol",
            "wire command request": r"\bFabricChatClefCommandRequest\b",
            "wire command result": r"\bFabricChatClefCommandResult\b",
            "wire result sender": r"\bFabricChatClefCommandResultSender\b",
            "wire result payload": r"\bFabricChatClefCommandResultPayload\b",
            "inventory rescan": r"\bgetItemCountInventoryOnly\s*\(",
            "world reread": r"\bgetBlockState\s*\(",
            "scanner reevaluation": r"\bisUnreachable\s*\(",
            "behavior task selection": r"\bTaskCatalogue\b",
            "blocking delay": r"\bThread\.sleep\s*\(",
            "raw output bypass": r"\bSystem\.(?:out|err)\b",
        }

        for boundary, pattern in forbidden_patterns.items():
            self.assertIsNone(re.search(pattern, text), boundary)

    def test_agent_command_utils_observes_the_existing_single_inventory_count(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "player2api"
            / "AgentCommandUtils.java"
        ).read_text(encoding="utf-8")

        self.assertEqual(1, text.count("getItemCountInventoryOnly("))
        self.assertIn(
            "import lavi.minecraft.diagnostics.crafting.acquisition.requirement.",
            text,
        )

        requested_index = text.index(
            "int requestedCount = target.getTargetCount();"
        )
        current_index = text.index("int currentItemCount =", requested_index)
        inventory_index = text.index("getItemCountInventoryOnly(", current_index)
        target_index = text.index(
            "int targetItemCount = requestedCount + currentItemCount;",
            inventory_index,
        )

        observer = re.search(
            r"\b(?:CraftResource|IronPickaxe)[A-Za-z0-9_]*(?:Diagnostics|Observer)\."
            r"[A-Za-z0-9_]*(?:observe|record|capture)[A-Za-z0-9_]*\s*\(",
            text[target_index:],
        )
        self.assertIsNotNone(observer, "the existing count decision must be observed")
        observer_index = target_index + observer.start()
        observer_end = text.index(");", observer_index) + 2
        observer_call = text[observer_index:observer_end]
        result_index = text.index(
            "resultTargets.add(new ItemTarget(target, targetItemCount));",
            observer_end,
        )

        self.assertLess(requested_index, current_index)
        self.assertLess(current_index, inventory_index)
        self.assertLess(inventory_index, target_index)
        self.assertLess(target_index, observer_index)
        self.assertLess(observer_index, result_index)
        for argument in (
            "target",
            "requestedCount",
            "currentItemCount",
            "targetItemCount",
        ):
            self.assertIn(argument, observer_call)

    def test_requirement_item_classification_runs_inside_the_diagnostic_guard(self):
        observer_text = (
            ACQUISITION_ROOT
            / "requirement"
            / "CraftResourceRequirementObserver.java"
        ).read_text(encoding="utf-8")
        source_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "player2api"
            / "AgentCommandUtils.java"
        ).read_text(encoding="utf-8")

        self.assertIn("Supplier<String>", observer_text)
        try_index = observer_text.index("try {")
        supplier_index = observer_text.index("requestedItemSupplier.get()")
        catch_index = observer_text.index("catch (", try_index)
        self.assertLess(try_index, supplier_index)
        self.assertLess(supplier_index, catch_index)
        self.assertIn("() -> target.matches(Items.IRON_PICKAXE)", source_text)

    def test_fabric_projection_cannot_requery_or_mutate_gameplay_state(self):
        text = self._projection_text()
        forbidden_patterns = {
            "inventory rescan": r"\bgetItemCountInventoryOnly\s*\(",
            "world reread": r"\bgetBlockState\s*\(",
            "scanner reevaluation": r"\bisUnreachable\s*\(",
            "task scheduling": r"\b(?:runUserTask|setTask|replaceTask)\s*\(",
            "input mutation": r"\bsetInputForceState\s*\(",
            "path mutation": r"\b(?:setGoalAndPath|cancelPath|cancelEverything)\s*\(",
            "behavior task selection": r"\bTaskCatalogue\b",
            "blocking delay": r"\bThread\.sleep\s*\(",
            "raw output bypass": r"\bSystem\.(?:out|err)\b",
        }

        for boundary, pattern in forbidden_patterns.items():
            self.assertIsNone(re.search(pattern, text), boundary)

    def test_authoritative_visible_task_log_precedes_requirement_projection(self):
        text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "tasktrace"
            / "VisibleTaskDiagnostics.java"
        ).read_text(encoding="utf-8")

        source_log = text.index("ChatClefDiagnostics.logBoundary(")
        projection = text.index(
            "CraftResourceRequirementSourceEventObserver.observeVisibleTaskReturn("
        )
        self.assertLess(source_log, projection)

    def test_projection_callbacks_require_completed_shared_source_emission(self):
        emitter_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "DiagnosticEventEmitter.java"
        ).read_text(encoding="utf-8")
        facade_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "ChatClefDiagnostics.java"
        ).read_text(encoding="utf-8")
        source_files = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "tasktrace"
            / "VisibleTaskDiagnostics.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "container"
            / "ContainerTaskDiagnostics.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "container"
            / "crafting"
            / "CraftingTableRouteRetryDiagnostics.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "container"
            / "furnace"
            / "FurnaceDiagnosticEmitter.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "MiningPathDiagnostics.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "BlockInteractionDiagnostics.java",
        )

        self.assertIn("DiagnosticDispatchResult emitEventWithOutcome(", emitter_text)
        self.assertIn("public static void logBoundary(", facade_text)
        self.assertIn(
            "public static boolean logBoundaryWithPhysicalOutcome(", facade_text
        )
        for path in source_files:
            text = path.read_text(encoding="utf-8")
            self.assertIn(
                "sourceEmissionCompleted",
                text,
                f"{path} must gate acquisition projection on physical source output",
            )

    def test_source_suppression_preserves_accounting_without_fabricating_lineage(self):
        status_path = (
            ACQUISITION_ROOT
            / "event"
            / "CraftResourceSourceEmissionStatus.java"
        )
        mining_path_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "MiningPathDiagnostics.java"
        ).read_text(encoding="utf-8")
        mining_observer_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "projection"
            / "MiningProjectionObserver.java"
        ).read_text(encoding="utf-8")
        mining_fields_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningEventFields.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(status_path.is_file(), str(status_path))
        status_text = status_path.read_text(encoding="utf-8")
        self.assertIn("EMISSION_CALLS_RETURNED", status_text)
        self.assertIn("SOURCE_DETAIL_NOT_EMITTED", status_text)
        self.assertIn("boolean sourceEmissionCompleted", mining_observer_text)
        self.assertNotRegex(
            mining_path_text,
            r"if\s*\(sourceEmissionCompleted\)\s*\{\s*"
            r"MiningProjectionObserverRegistry\.observe",
        )
        self.assertIn("sourceEmissionCompleted", mining_path_text)
        self.assertIn('"sourceEventEmissionStatus"', mining_fields_text)

    def test_failure_aggregate_retains_existing_unreachable_and_blacklist_facts(self):
        failure_root = ACQUISITION_ROOT / "target" / "failure"
        mining_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        summary_text = self._projection_text()

        self.assertTrue(
            (failure_root / "CraftResourceFailureAggregateLedger.java").is_file()
        )
        self.assertTrue(
            (failure_root / "CraftResourceFailureAggregateSnapshot.java").is_file()
        )
        for field in (
            "failureCountBefore",
            "failureCountAfter",
            "allowedFailuresAfter",
            "unreachableBefore",
            "unreachableAfter",
            "sourceEmissionCompleted",
        ):
            self.assertIn(field, mining_text)
        for field in (
            '"ownerTaskClass"',
            '"targetAttemptSequence"',
            '"unreachableRequestCount"',
            '"firstFailureCount"',
            '"lastFailureCount"',
            '"allowedFailures"',
            '"unreachableBefore"',
            '"unreachableAfter"',
            '"blacklistTransitionCount"',
            '"firstObservedTick"',
            '"lastObservedTick"',
            '"suppressedDetailCount"',
        ):
            self.assertIn(field, summary_text)

    def test_terminal_decision_is_classification_not_a_fabricated_primary_cause(self):
        lifecycle_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalLifecycleObserver.java"
        ).read_text(encoding="utf-8")
        decision_start = lifecycle_text.index("private static void observeTerminalDecision(")
        decision_end = lifecycle_text.index(
            "private static void observeTerminalResultSent(", decision_start
        )
        decision_body = lifecycle_text[decision_start:decision_end]

        self.assertNotIn("recordPrimaryCause(", decision_body)
        self.assertNotIn("EXISTING_TERMINAL_DECISION", lifecycle_text)
        self.assertIn("recordClassification(", decision_body)

    def test_detached_context_is_frozen_before_task_finished_classification(self):
        lifecycle_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalLifecycleObserver.java"
        ).read_text(encoding="utf-8")
        task_start = lifecycle_text.index("private static void observeTaskFinished(")
        task_end = lifecycle_text.index(
            "private static boolean matchesExecutionRoot(", task_start
        )
        task_body = lifecycle_text[task_start:task_end]

        self.assertIn('"detached"', task_body)
        self.assertIn('"detached_reason"', task_body)
        self.assertLess(task_body.index("recordDetach("), task_body.index("recordTaskFinished("))

    def test_incomplete_terminal_retention_is_reported_as_partial_capture(self):
        text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "required"
            / "FabricChatClefCraftResourceTerminalLifecycleRequiredFields.java"
        ).read_text(encoding="utf-8")

        self.assertIn("CraftResourceCoverageStatus.COMPLETE", text)
        self.assertIn('? "complete"', text)
        self.assertIn(': "partial"', text)

    def test_detach_event_without_cas_result_cannot_claim_lifecycle_clear(self):
        lifecycle_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalLifecycleObserver.java"
        ).read_text(encoding="utf-8")
        detach_start = lifecycle_text.index("private static void observeDetach(")
        detach_body = lifecycle_text[detach_start:]

        self.assertIn("recordDetach(", detach_body)
        self.assertNotIn("recordLifecycleCleared(", detach_body)
        self.assertIn("compare-and-set", detach_body)

    def test_candidate_reference_detail_has_an_independent_sixty_four_transition_cap(self):
        registry_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "reference"
            / "FabricChatClefCraftResourceReferenceRegistry.java"
        ).read_text(encoding="utf-8")
        support_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "common"
            / "FabricChatClefCraftResourceContainerProjectionSupport.java"
        ).read_text(encoding="utf-8")

        self.assertIn("DETAIL_TRANSITION_LIMIT = 64", registry_text)
        self.assertIn("detailEligible()", support_text)

    def test_mismatch_requires_same_boundary_active_tuple_and_matching_attempt_tuple(self):
        projection_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        fields_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningEventFields.java"
        ).read_text(encoding="utf-8")

        active_guard = projection_text.index("if (!activeMiningTupleProven)")
        mismatch_evaluation = projection_text.index("registry.evaluateMismatch(")
        self.assertLess(active_guard, mismatch_evaluation)
        self.assertIn("currentTuple().filter(activeTuple::equals)", fields_text)

    def test_projection_failures_are_retained_as_bounded_coverage_gaps(self):
        association_text = (
            FABRIC_PROJECTION_ROOT
            / "association"
            / "FabricChatClefCraftResourceAssociationReader.java"
        ).read_text(encoding="utf-8")
        mining_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        summary_text = self._projection_text()

        self.assertIn("observeProjectionFailure(", association_text)
        self.assertIn("observeProjectionFailure(", mining_text)
        self.assertIn('"associationObservationGapCount"', summary_text)
        self.assertIn('"lastAssociationObservationGapReason"', summary_text)

    def test_null_emitting_task_is_never_replaced_by_the_ambient_task(self):
        association_text = (
            FABRIC_PROJECTION_ROOT
            / "association"
            / "FabricChatClefCraftResourceAssociationReader.java"
        ).read_text(encoding="utf-8")

        null_guard = association_text.index("if (emittingTask == null)")
        engine_read = association_text.index("AltoClef mod = AltoClef.getInstance()")
        self.assertLess(null_guard, engine_read)
        self.assertIn('"EMITTING_TASK_UNAVAILABLE"', association_text)
        self.assertNotIn(
            "? ChatClefDiagnostics.currentTaskForDiagnostics()",
            association_text,
        )

    def test_terminal_retained_text_uses_a_utf8_bounded_terminal_owner(self):
        ledger_text = (
            ACQUISITION_ROOT
            / "terminal"
            / "CraftResourceTerminalLedger.java"
        ).read_text(encoding="utf-8")
        binding_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalScopeBinding.java"
        ).read_text(encoding="utf-8")

        self.assertIn("CraftResourceTerminalTextBound", ledger_text)
        self.assertIn("CraftResourceTerminalTextBound", binding_text)

    def test_reconciliation_closes_only_a_proven_replaced_active_target(self):
        text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("previousActiveTargetProven", text)
        self.assertIn("replacementApplied", text)
        self.assertIn("previousChildStopCalled", text)
        self.assertIn("CraftResourceTargetObservationKind.TARGET_ABANDONED", text)

    def test_requested_block_projection_has_a_fail_closed_input_bound(self):
        text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningTargetAdapter.java"
        ).read_text(encoding="utf-8")

        self.assertIn("EXPECTED_BLOCK_INPUT_LIMIT", text)
        self.assertIn("expectedBlockIdsInputComplete", text)
        self.assertRegex(
            text,
            r"Math\.min\(\s*requestedBlocks\.length,\s*"
            r"EXPECTED_BLOCK_INPUT_LIMIT\s*\)",
        )

    def test_mismatch_coverage_gaps_are_retained_for_terminal_evidence(self):
        tracker_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceMismatchAdmissionTracker.java"
        ).read_text(encoding="utf-8")
        summary_text = self._projection_text()

        self.assertIn("coverageGapCount", tracker_text)
        self.assertIn('"sessionMismatchCoverageGapCount"', summary_text)

    def test_mismatch_fingerprints_are_semantic_and_correlation_state_is_bounded(self):
        tracker_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceMismatchAdmissionTracker.java"
        ).read_text(encoding="utf-8")
        contract_text = (
            ACQUISITION_ROOT
            / "event"
            / "CraftResourceAcquisitionEventContract.java"
        ).read_text(encoding="utf-8")
        registry_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetDiagnosticsRegistry.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ACTIVE_CORRELATION_LIMIT = 8", tracker_text)
        self.assertIn("activateCorrelation(", tracker_text)
        self.assertIn("retireCorrelation(", tracker_text)
        self.assertIn("sessionSignatures.contains(fingerprint)", tracker_text)
        self.assertNotIn(
            'appendPart(value, observation.commandCorrelationId())',
            tracker_text,
        )
        self.assertNotIn('"commandCorrelationId",', contract_text)
        self.assertIn("mismatchTracker.activateCorrelation(", registry_text)
        self.assertIn("mismatchTracker.retireCorrelation(", registry_text)

    def test_lifecycle_details_are_snapshotted_once_for_authority_and_projection(self):
        diagnostics_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "fabric"
            / "chatclef"
            / "bridge"
            / "command"
            / "diagnostics"
            / "FabricChatClefCommandDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("FabricChatClefCommandDiagnosticDetailsSnapshot.capture", diagnostics_text)
        self.assertNotIn("details.toMap()", diagnostics_text)

    def test_admission_and_physical_completion_are_recorded_separately(self):
        facade_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "ChatClefDiagnostics.java"
        ).read_text(encoding="utf-8")
        emitter_text = (
            ACQUISITION_ROOT
            / "event"
            / "CraftResourceAcquisitionEventEmitter.java"
        ).read_text(encoding="utf-8")
        registry_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetDiagnosticsRegistry.java"
        ).read_text(encoding="utf-8")
        mining_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        terminal_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalDiagnostics.java"
        ).read_text(encoding="utf-8")
        self.assertIn(
            "public static DiagnosticDispatchResult logBoundedBoundaryWithDispatchResult(",
            facade_text,
        )
        dispatch_start = facade_text.index(
            "public static DiagnosticDispatchResult "
            "logBoundedBoundaryWithDispatchResult("
        )
        dispatch_end = facade_text.index("\n    }", dispatch_start)
        dispatch_body = facade_text[dispatch_start:dispatch_end]
        self.assertNotIn("SESSION.isEligible()", dispatch_body)
        self.assertIn("public DiagnosticDispatchResult emitWithDispatchResult(", emitter_text)
        self.assertIn("recordMismatchAdmissionDenied(", registry_text)
        self.assertIn("DiagnosticDispatchResult mismatchDispatchResult", mining_text)
        self.assertIn("!mismatchDispatchResult.admitted()", mining_text)
        self.assertIn("mismatchDispatchResult.emissionCompleted()", mining_text)
        self.assertIn("recordMismatchAdmissionDenied(", mining_text)
        self.assertIn("DiagnosticDispatchResult terminalDispatchResult", terminal_text)
        self.assertIn("terminalDispatchResult.admitted()", terminal_text)
        self.assertIn("terminalDispatchResult.emissionCompleted()", terminal_text)
        self.assertIn("TERMINALS.recordAdmissionOutcome(", terminal_text)
        payload_text = self._projection_text()
        self.assertIn('fields.put("terminalEmissionAdmitted", true)', payload_text)
        self.assertNotIn("UNAVAILABLE_SHARED_VOID_EMITTER", payload_text)

    def test_terminal_reconstruction_envelope_is_required_and_folderized(self):
        payload_root = FABRIC_PROJECTION_ROOT / "terminal" / "payload"
        required_root = payload_root / "required"
        emitter_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalSummaryEmitter.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(
            (payload_root / "FabricChatClefCraftResourceTerminalPayload.java").is_file()
        )
        self.assertTrue(
            (
                payload_root
                / "FabricChatClefCraftResourceTerminalPayloadAssembler.java"
            ).is_file()
        )
        required_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(required_root.rglob("*.java"))
        )
        required_keys = {
            "commandRequestId",
            "commandCorrelationId",
            "commandSessionId",
            "commandConnectionGeneration",
            "rootAssignmentId",
            "requestedItem",
            "requestedCount",
            "currentItemCountAtStart",
            "currentItemCountAtTerminal",
            "targetItemCount",
            "naturalTaskFinished",
            "thisOrChildTimedOutAtFinalization",
            "thisOrChildTimedOutEverObserved",
            "firstTimedOutObservationTick",
            "lastTimedOutObservationTick",
            "cancelInvocationId",
            "connectionDetached",
            "detachReason",
            "primaryTerminationCause",
            "taskTerminationKind",
            "terminalDecisionReason",
            "classifiedResultStatus",
            "classifiedResultReason",
            "classifiedResultFidelity",
            "evidenceConclusion",
            "terminalSent",
            "resultSendStatus",
            "resultDeliveryStatus",
            "lifecycleCleared",
            "queueContextCleared",
            "contextUnbindReason",
            "finalizationBoundary",
            "finalizationMode",
            "terminalEmissionAttempted",
            "terminalEmissionAdmitted",
            "elapsedTicks",
            "elapsedMs",
            "requirementTransitionCount",
            "targetAttemptCount",
            "boundedTargetHistory",
            "unreachableRequestCount",
            "blacklistTransitionCount",
            "chainOwnerTransitionCount",
            "commandDescendantObservationCount",
            "unownedObservationCount",
            "unknownAssociationCount",
            "blockOptionalMetaExceptionCount",
            "blockOptionalMetaCoverageGapCount",
            "lastSuccessfulBoundary",
            "firstExplicitFailureBoundary",
            "firstUnobservedBoundaryAfter",
            "suppressedDetailCount",
            "coverageStatus",
        }
        for key in required_keys:
            self.assertIn(f'"{key}"', required_text, key)
        self.assertIn("PAYLOADS.assemble(", emitter_text)
        self.assertIn("payload.requiredFieldArray()", emitter_text)
        self.assertIn("payload.optionalFieldArray()", emitter_text)

    def test_mismatch_terminal_evidence_is_isolated_by_exact_command_scope(self):
        command_root = ACQUISITION_ROOT / "target" / "mismatch" / "command"
        registry_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetDiagnosticsRegistry.java"
        ).read_text(encoding="utf-8")
        evidence_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalEvidence.java"
        ).read_text(encoding="utf-8")
        payload_text = self._projection_text()

        self.assertTrue(
            (command_root / "CraftResourceCommandMismatchLedger.java").is_file()
        )
        self.assertTrue(
            (command_root / "CraftResourceCommandMismatchSnapshot.java").is_file()
        )
        self.assertIn(
            "Map<IronPickaxeAcquisitionScopeKey, CraftResourceCommandMismatchLedger>",
            registry_text,
        )
        self.assertIn("commandMismatchSnapshot(", registry_text)
        self.assertIn("commandMismatchSnapshot", evidence_text)
        self.assertIn('"mismatchAggregateScope", "EXACT_COMMAND_SCOPE"', payload_text)
        self.assertIn('"commandMismatchAdmissionDeniedCount"', payload_text)
        self.assertIn('"commandMismatchEmissionFailureCount"', payload_text)

    def test_interaction_references_existing_attempt_without_mutating_target_ledger(self):
        interaction_root = FABRIC_PROJECTION_ROOT / "interaction"
        reference_root = interaction_root / "reference"
        observer_text = (
            interaction_root
            / "FabricChatClefCraftResourceInteractionObserver.java"
        ).read_text(encoding="utf-8")
        classifier_text = (
            interaction_root
            / "FabricChatClefCraftResourceInteractionTargetClassifier.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(
            (
                reference_root
                / "FabricChatClefCraftResourceInteractionAttemptReference.java"
            ).is_file()
        )
        self.assertTrue(
            (
                reference_root
                / "FabricChatClefCraftResourceInteractionReferenceRegistry.java"
            ).is_file()
        )
        self.assertNotIn("CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN", observer_text)
        self.assertNotIn("new CraftResourceTargetTuple(", observer_text)
        self.assertIn("registry.currentSnapshot(binding.key())", observer_text)
        self.assertIn("REFERENCES.begin(", observer_text)
        self.assertIn("REFERENCES.complete(context.interactionId())", observer_text)
        self.assertIn("CONTAINER_OPEN_ATTEMPT_OBSERVED", observer_text)
        self.assertIn("CONTAINER_OPEN_RETURN_OBSERVED", observer_text)
        self.assertIn("CraftResourceStage.UNKNOWN", classifier_text)
        self.assertIn("CraftResourceTargetRole.UNKNOWN", classifier_text)

    def test_interaction_ownership_uses_one_shot_exact_task_evidence(self):
        owner_root = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "interaction"
            / "ownership"
        )
        registry_text = (
            owner_root / "BlockInteractionOwnerTokenRegistry.java"
        ).read_text(encoding="utf-8")
        context_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "interaction"
            / "BlockInteractionContext.java"
        ).read_text(encoding="utf-8")
        task_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "InteractWithBlockTask.java"
        ).read_text(encoding="utf-8")
        observer_text = (
            FABRIC_PROJECTION_ROOT
            / "interaction"
            / "FabricChatClefCraftResourceInteractionObserver.java"
        ).read_text(encoding="utf-8")

        self.assertTrue((owner_root / "BlockInteractionOwnerToken.java").is_file())
        self.assertIn("ThreadLocal<", registry_text)
        self.assertIn("MAX_TOKEN_AGE_TICKS = 1L", registry_text)
        self.assertIn("current.token = null", registry_text)
        self.assertIn("token.targetPosition().equals(observedTargetPosition)", registry_text)
        self.assertIn("Task sourceTask", context_text)
        self.assertIn("public Task sourceTask()", context_text)
        held_read = task_text.index("boolean interactHeld =")
        owner_publish = task_text.index(
            "ChatClefDiagnostics.noteBlockInteractionOwner(this, target)"
        )
        self.assertLess(held_read, owner_publish)
        self.assertIn(
            "FabricChatClefCraftResourceAssociationReader.capture(\n"
            "                        context.sourceTask()",
            observer_text,
        )
        self.assertNotIn("capture(null)", observer_text)
        self.assertIn("REFERENCES.complete(context.interactionId())", observer_text)

    def test_interaction_reference_requires_the_complete_active_target_tuple(self):
        interaction_root = FABRIC_PROJECTION_ROOT / "interaction"
        observer_text = (
            interaction_root / "FabricChatClefCraftResourceInteractionObserver.java"
        ).read_text(encoding="utf-8")
        matcher_text = (
            interaction_root
            / "matching"
            / "FabricChatClefCraftResourceInteractionTupleMatcher.java"
        ).read_text(encoding="utf-8")
        tuple_text = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetTuple.java"
        ).read_text(encoding="utf-8")

        self.assertIn("MATCHER.evaluate(", observer_text)
        self.assertIn("expected.resourceStage()", matcher_text)
        self.assertIn("expected.targetRole()", matcher_text)
        self.assertIn("CraftResourceTargetPosition.canonicalize", matcher_text)
        self.assertIn("expected.expectedBlockIds()", matcher_text)
        self.assertIn("observedBlockId", matcher_text)
        self.assertIn("CraftResourceStage.UNKNOWN", matcher_text)
        self.assertIn("CraftResourceTargetRole.UNKNOWN", matcher_text)
        self.assertIn("CraftResourceTargetPosition.canonicalize", tuple_text)

    def test_container_attempt_starts_only_after_exact_child_installation(self):
        activation_root = FABRIC_PROJECTION_ROOT / "container" / "activation"
        activation_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(activation_root.glob("*.java"))
        )
        support_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "common"
            / "FabricChatClefCraftResourceContainerProjectionSupport.java"
        ).read_text(encoding="utf-8")
        mining_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        interaction_text = (
            FABRIC_PROJECTION_ROOT
            / "interaction"
            / "FabricChatClefCraftResourceInteractionObserver.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(activation_root.is_dir())
        self.assertTrue(
            (
                activation_root
                / "FabricChatClefCraftResourceContainerActivationRegistry.java"
            ).is_file()
        )
        self.assertIn("ACTIVE_SCOPE_LIMIT = 8", activation_text)
        self.assertIn("pending.parentTask() == parent", activation_text)
        self.assertIn("pending.candidateTask() == candidateChild", activation_text)
        self.assertIn("activeChildAfter == candidateChild", activation_text)
        self.assertIn("replacementApplied", activation_text)
        self.assertIn("sourceEmissionCompleted", activation_text)
        self.assertIn("CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN", activation_text)
        self.assertIn("observeCandidate(", support_text)
        self.assertNotIn(
            "CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN",
            support_text,
        )
        self.assertNotIn(
            "CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN",
            interaction_text,
        )
        self.assertIn(
            "CraftResourceTargetObservationKind.ACTIVE_TARGET_PROVEN",
            mining_text,
        )

    def test_container_reconciliation_has_a_physical_source_event_for_both_paths(self):
        source_root = (
            ACQUISITION_ROOT
            / "source"
            / "container"
            / "reconciliation"
        )
        source_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(source_root.glob("*.java"))
        )
        task_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasksystem"
            / "Task.java"
        ).read_text(encoding="utf-8")
        components_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "fabric"
            / "chatclef"
            / "bridge"
            / "runtime"
            / "FabricChatClefBridgeComponents.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(source_root.is_dir())
        self.assertIn("parent instanceof DoStuffInContainerTask", source_text)
        self.assertIn('"CONTAINER_TASK_CHILD_RECONCILIATION"', source_text)
        self.assertIn("logBoundaryWithPhysicalOutcome", source_text)
        self.assertIn("sourceEmissionCompleted", source_text)
        self.assertEqual(
            2,
            task_text.count("ContainerTaskDiagnostics.logChildReconciliation("),
        )
        self.assertIn(
            "CraftResourceContainerReconciliationSourceEventObserver.install(",
            components_text,
        )

    def test_container_activation_registry_is_released_with_its_command_scope(self):
        support_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "common"
            / "FabricChatClefCraftResourceContainerProjectionSupport.java"
        ).read_text(encoding="utf-8")

        retire_start = support_text.index(
            "public static void retire(IronPickaxeAcquisitionScopeKey key)"
        )
        clear_start = support_text.index("public static void clearForModeOff()")
        retire_body = support_text[retire_start:clear_start]
        clear_body = support_text[clear_start:]

        self.assertIn(
            "FabricChatClefCraftResourceContainerActivationDiagnostics.retire(key)",
            retire_body,
        )
        self.assertIn(
            "FabricChatClefCraftResourceContainerActivationDiagnostics.clearForModeOff()",
            clear_body,
        )

    def test_container_activation_cannot_project_a_suppressed_reconciliation_source(self):
        activation_root = FABRIC_PROJECTION_ROOT / "container" / "activation"
        registry_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationRegistry.java"
        ).read_text(encoding="utf-8")
        decision_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationDecision.java"
        ).read_text(encoding="utf-8")
        diagnostics_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationDiagnostics.java"
        ).read_text(encoding="utf-8")
        event_fields_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationEventFields.java"
        ).read_text(encoding="utf-8")

        self.assertIn("boolean sourceEmissionCompleted", decision_text)
        self.assertIn("boolean sourceTransitionSuppressed", decision_text)
        self.assertIn("sourceTransitionSuppressed", registry_text)
        self.assertIn("activation.sourceTransitionSuppressed()", diagnostics_text)
        self.assertIn("activation.sourceEmissionCompleted()", diagnostics_text)
        self.assertIn("boolean sourceEmissionCompleted", event_fields_text)
        self.assertNotIn(
            'fields.put("reconciliationSourceEmissionCompleted", true)',
            event_fields_text,
        )

    def test_container_activation_consumes_stale_exact_task_references_fail_closed(self):
        activation_root = FABRIC_PROJECTION_ROOT / "container" / "activation"
        registry_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationRegistry.java"
        ).read_text(encoding="utf-8")
        decision_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationDecision.java"
        ).read_text(encoding="utf-8")
        diagnostics_text = (
            activation_root
            / "FabricChatClefCraftResourceContainerActivationDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("boolean identityMismatchObserved", decision_text)
        self.assertIn("boolean pendingBelongsToParent", registry_text)
        pending_branch = registry_text.index("if (pendingBelongsToParent)")
        pending_clear = registry_text.index("state.pending = null", pending_branch)
        decision_return = registry_text.index(
            "return new FabricChatClefCraftResourceContainerActivationDecision(",
            pending_clear,
        )
        self.assertLess(pending_clear, decision_return)
        self.assertIn("boolean activeIdentityMismatch", registry_text)
        self.assertIn("activation.identityMismatchObserved()", diagnostics_text)
        self.assertIn("canActivateCandidate(", registry_text)
        self.assertIn("if (!ACTIVATIONS.canActivateCandidate(", diagnostics_text)
        self.assertIn("if (!ACTIVATIONS.markActive(", diagnostics_text)

    def test_container_closure_requires_the_exact_active_attempt_sequence(self):
        activation_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "activation"
            / "FabricChatClefCraftResourceContainerActivationDiagnostics.java"
        ).read_text(encoding="utf-8")
        lifecycle_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "lifecycle"
            / "FabricChatClefCraftResourceContainerLifecycleDiagnostics.java"
        ).read_text(encoding="utf-8")

        for text in (activation_text, lifecycle_text):
            self.assertIn("currentSnapshot", text)
            self.assertIn("active.targetAttemptSequence()", text)
            self.assertIn("targetAttemptSequence()", text)

    def test_task_scheduler_imports_the_container_diagnostics_facade(self):
        task_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasksystem"
            / "Task.java"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "import lavi.minecraft.diagnostics.container.ContainerTaskDiagnostics;",
            task_text,
        )

    def test_container_owner_stop_has_a_physical_exact_owner_closure_source(self):
        lifecycle_source_root = (
            ACQUISITION_ROOT
            / "source"
            / "container"
            / "lifecycle"
        )
        self.assertTrue(lifecycle_source_root.is_dir(), str(lifecycle_source_root))
        lifecycle_source_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(lifecycle_source_root.glob("*.java"))
        )
        owner_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "container"
            / "DoStuffInContainerTask.java"
        ).read_text(encoding="utf-8")
        components_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "fabric"
            / "chatclef"
            / "bridge"
            / "runtime"
            / "FabricChatClefBridgeComponents.java"
        ).read_text(encoding="utf-8")
        lifecycle_projection_root = (
            FABRIC_PROJECTION_ROOT / "container" / "lifecycle"
        )
        self.assertTrue(
            lifecycle_projection_root.is_dir(),
            str(lifecycle_projection_root),
        )
        lifecycle_projection_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(lifecycle_projection_root.glob("*.java"))
        )
        activation_registry_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "activation"
            / "FabricChatClefCraftResourceContainerActivationRegistry.java"
        ).read_text(encoding="utf-8")

        self.assertIn('"CONTAINER_TASK_OWNER_STOP"', lifecycle_source_text)
        self.assertIn("logBoundaryWithPhysicalOutcome", lifecycle_source_text)
        self.assertIn("sourceEmissionCompleted", lifecycle_source_text)
        owner_interest = lifecycle_source_text.index("isOwnerTracked(owner)")
        physical_source = lifecycle_source_text.index(
            "logBoundaryWithPhysicalOutcome",
            owner_interest,
        )
        self.assertLess(owner_interest, physical_source)
        self.assertIn("CraftResourceTargetObservationKind.OWNER_STOP", lifecycle_source_text)
        self.assertIn(
            "CraftResourceTargetObservationKind.OWNER_INTERRUPT",
            lifecycle_source_text,
        )
        on_stop = owner_text.index("protected void onStop(Task interruptTask)")
        owner_callback = owner_text.index(
            "ContainerTaskDiagnostics.logOwnerStop(this, interruptTask);",
            on_stop,
        )
        behavior_pop = owner_text.index("getBehaviour().pop()", owner_callback)
        self.assertLess(owner_callback, behavior_pop)
        self.assertIn(
            "CraftResourceContainerLifecycleSourceEventObserver.install(",
            components_text,
        )
        self.assertIn("observeOwnerExit(", activation_registry_text)
        self.assertIn("hasOwner(", activation_registry_text)
        self.assertIn("ownerIdentityAmbiguous", activation_registry_text)
        self.assertIn("ambiguousScopeKeys", activation_registry_text)
        self.assertIn(
            "CraftResourceTargetObservationKind.OWNER_STOP",
            lifecycle_projection_text,
        )
        self.assertIn(
            "CraftResourceTargetObservationKind.OWNER_INTERRUPT",
            lifecycle_projection_text,
        )
        self.assertIn("sourceEmissionCompleted", lifecycle_projection_text)
        self.assertIn("active.parentTask() == owner", activation_registry_text)
        self.assertIn("IronPickaxeAcquisitionScopeDiagnostics.activeBinding(", lifecycle_projection_text)
        self.assertNotIn("AssociationReader.capture(owner)", lifecycle_projection_text)
        self.assertIn("SEALED_EXACT_OWNER_FROM_APPLIED_RECONCILIATION", lifecycle_projection_text)
        self.assertIn("ownerExit.ambiguousScopeKeys()", lifecycle_projection_text)
        self.assertIn("CONTAINER_OWNER_IDENTITY_AMBIGUOUS_ACROSS_SCOPES", lifecycle_projection_text)

    def test_crafting_table_candidate_uses_exact_task_not_summary_parsing(self):
        task_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "container"
            / "DoStuffInContainerTask.java"
        ).read_text(encoding="utf-8")
        generic_text = (
            FABRIC_PROJECTION_ROOT
            / "container"
            / "generic"
            / "FabricChatClefCraftResourceContainerProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")

        open_branch = task_text.index('"return_open_table_task"')
        selected_child = task_text.index(
            "ContainerTaskDiagnostics.logBoundaryWithSelectedChild(",
            open_branch,
        )
        open_return = task_text.index("return openTableTask;", selected_child)
        self.assertLess(selected_child, open_return)
        self.assertIn("openTableTask,", task_text[selected_child:open_return])
        self.assertIn("void onChildSelection(", generic_text)
        self.assertIn("Task candidateChild", generic_text)
        self.assertNotIn("taskSummary(", generic_text)
        self.assertNotIn("candidateChildInstanceId", generic_text)

    def test_target_attempt_closes_only_on_observed_abandonment_or_terminal(self):
        mine_task_text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "resources"
            / "MineAndCollectTask.java"
        ).read_text(encoding="utf-8")
        mining_path_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "MiningPathDiagnostics.java"
        ).read_text(encoding="utf-8")
        observer_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "projection"
            / "MiningProjectionObserver.java"
        ).read_text(encoding="utf-8")
        projection_text = (
            FABRIC_PROJECTION_ROOT
            / "target"
            / "FabricChatClefCraftResourceMiningProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        terminal_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalDiagnostics.java"
        ).read_text(encoding="utf-8")

        progress_start = mine_task_text.index(
            '"MINE_OR_COLLECT_PROGRESS_FAILURE"'
        )
        progress_end = mine_task_text.index("return super.onTick()", progress_start)
        progress_body = mine_task_text[progress_start:progress_end]
        self.assertLess(
            progress_body.index("miningPos = null"),
            progress_body.index("logMineTargetAbandoned("),
        )
        self.assertIn("logMineTargetAbandoned(", mining_path_text)
        self.assertIn("observeMineTargetAbandoned(", observer_text)
        self.assertIn("CraftResourceTargetObservationKind.TARGET_ABANDONED", projection_text)
        self.assertIn("CraftResourceTargetObservationKind.OWNER_STOP", projection_text)
        self.assertIn("CraftResourceTargetObservationKind.OWNER_INTERRUPT", projection_text)
        self.assertIn("closeForCommandTerminal(binding.scopeKey())", terminal_text)
        self.assertLess(
            terminal_text.index("closeForCommandTerminal(binding.scopeKey())"),
            terminal_text.index("EVIDENCE.assemble(binding)"),
        )

    def test_connection_detach_observation_is_independent_from_primary_cause(self):
        ledger_text = (
            ACQUISITION_ROOT
            / "terminal"
            / "CraftResourceTerminalLedger.java"
        ).read_text(encoding="utf-8")
        snapshot_text = (
            ACQUISITION_ROOT
            / "terminal"
            / "CraftResourceTerminalSnapshot.java"
        ).read_text(encoding="utf-8")
        lifecycle_fields_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "required"
            / "FabricChatClefCraftResourceTerminalLifecycleRequiredFields.java"
        ).read_text(encoding="utf-8")

        self.assertIn("connectionDetachedObserved", ledger_text)
        self.assertIn("boolean connectionDetachedObserved", snapshot_text)
        self.assertIn(
            'fields.put("connectionDetached", snapshot.connectionDetachedObserved())',
            lifecycle_fields_text,
        )
        connection_start = lifecycle_fields_text.index(
            'fields.put("connectionDetached"'
        )
        connection_end = lifecycle_fields_text.index(";", connection_start)
        self.assertNotIn(
            "CraftResourcePrimaryTerminationCause.CONNECTION_DETACH_CANCEL",
            lifecycle_fields_text[connection_start:connection_end],
        )

    def test_requirement_transitions_use_a_command_scoped_semantic_ledger(self):
        progress_root = ACQUISITION_ROOT / "requirement" / "progress"
        fabric_progress = (
            FABRIC_PROJECTION_ROOT
            / "requirement"
            / "progress"
            / "FabricChatClefCraftResourceRequirementProgressDiagnostics.java"
        )
        terminal_required = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "required"
            / "FabricChatClefCraftResourceTerminalRequirementRequiredFields.java"
        ).read_text(encoding="utf-8")
        terminal_evidence = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "FabricChatClefCraftResourceTerminalEvidence.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(
            (progress_root / "CraftResourceRequirementProgressLedger.java").is_file()
        )
        self.assertTrue(
            (progress_root / "CraftResourceRequirementProgressSnapshot.java").is_file()
        )
        self.assertTrue(
            (progress_root / "CraftResourceRequirementProgressRegistry.java").is_file()
        )
        self.assertTrue(fabric_progress.is_file())
        progress_text = fabric_progress.read_text(encoding="utf-8")
        self.assertIn("observeMaterialProgress(", progress_text)
        self.assertIn("observeOperationGate(", progress_text)
        self.assertIn("sourceEmissionCompleted", progress_text)
        self.assertIn("requirementProgressSnapshot", terminal_evidence)
        self.assertIn(
            "progress.requirementTransitionCount()",
            terminal_required,
        )
        self.assertNotIn(
            "requirement.requestedCount().isPresent() ? 1 : 0",
            terminal_required,
        )
        initial_method = progress_text[
            progress_text.index("observeInitialDecision("):
            progress_text.index("observeMaterialProgress(")
        ]
        self.assertIn("emitTransitionDetail", progress_text)
        self.assertRegex(
            initial_method,
            r"sourceEmissionCompleted\s*,\s*false",
        )
        self.assertIn("!emitTransitionDetail", progress_text)

    def test_bounded_formatter_never_truncates_validated_opaque_identity_fields(self):
        formatter = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "formatting"
            / "DiagnosticBoundedEventFormatter.java"
        ).read_text(encoding="utf-8")

        self.assertIn("EXACT_REQUIRED_VALUE_KEYS", formatter)
        for key in (
            "commandRequestId",
            "commandCorrelationId",
            "commandSessionId",
            "rootAssignmentId",
            "boundRootTaskInstanceId",
        ):
            self.assertIn(f'"{key}"', formatter)
        self.assertIn("preserveExactValue", formatter)
        self.assertIn("REQUIRED_VALUE_LIMITS_BYTES", formatter)

    def test_actual_terminal_required_key_set_fits_with_five_worst_case_ids(self):
        formatter_path = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "formatting"
            / "DiagnosticBoundedEventFormatter.java"
        )
        formatter = formatter_path.read_text(encoding="utf-8")
        limit_match = re.search(
            r"REQUIRED_VALUE_LIMITS_BYTES\s*=\s*\{([^}]+)\}",
            formatter,
        )
        self.assertIsNotNone(limit_match)
        value_limits = [
            int(value.strip())
            for value in limit_match.group(1).split(",")
        ]
        smallest_value_limit = min(value_limits)

        required_root = FABRIC_PROJECTION_ROOT / "terminal" / "payload" / "required"
        required_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted(required_root.rglob("*.java"))
        )
        required_keys = set(
            re.findall(r'fields\.put\(\s*"([A-Za-z0-9_]+)"', required_text)
        )
        self.assertGreaterEqual(len(required_keys), 90)
        exact_keys = {
            "commandRequestId",
            "commandCorrelationId",
            "commandSessionId",
            "rootAssignmentId",
            "boundRootTaskInstanceId",
        }
        base_keys = (
            "traceId",
            "clientTickId",
            "eventSequence",
            "taskInstanceId",
            "taskRunId",
            "parentTaskRunId",
            "threadName",
            "level",
            "event",
            "reason",
            "taskClass",
        )
        physical_bytes = len("ALTO CLEF: [LAVI ChatClefBoundary] ".encode("utf-8"))
        physical_bytes += sum(
            len(key.encode("utf-8")) + 1 + smallest_value_limit + 1
            for key in base_keys
        )
        for key in required_keys:
            if key in exact_keys:
                value_bytes = 1080
            elif key == "diagnosticCaptureStatus":
                value_bytes = len("partial")
            else:
                value_bytes = smallest_value_limit
            physical_bytes += len(key.encode("utf-8")) + 1 + value_bytes + 1

        self.assertLessEqual(physical_bytes, 8192)
        requirement_projection = (
            FABRIC_PROJECTION_ROOT
            / "requirement"
            / "FabricChatClefIronPickaxeRequirementProjectionDiagnostics.java"
        ).read_text(encoding="utf-8")
        self.assertRegex(
            requirement_projection,
            r'required\.put\(\s*"boundRootTaskInstanceId"',
        )
        self.assertNotRegex(
            requirement_projection,
            r'required\.put\(\s*"rootTaskInstanceId"',
        )

    def test_terminal_closure_retains_target_and_failure_attribution(self):
        target_snapshot = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetAttemptSnapshot.java"
        ).read_text(encoding="utf-8")
        target_ledger = (
            ACQUISITION_ROOT
            / "target"
            / "CraftResourceTargetAttemptLedger.java"
        ).read_text(encoding="utf-8")
        terminal_target = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "required"
            / "target"
            / "FabricChatClefCraftResourceTerminalTargetAttemptRequiredFields.java"
        ).read_text(encoding="utf-8")
        terminal_failure = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "required"
            / "target"
            / "FabricChatClefCraftResourceTerminalFailureRequiredFields.java"
        ).read_text(encoding="utf-8")

        self.assertTrue(
            (
                ACQUISITION_ROOT
                / "target"
                / "closure"
                / "CraftResourceTargetClosureSnapshot.java"
            ).is_file()
        )
        self.assertIn("lastClosure", target_snapshot)
        self.assertIn("lastClosure", target_ledger)
        self.assertIn("target.lastClosure()", terminal_target)
        for field in (
            "failureTargetAttemptSequence",
            "failureResourceStage",
            "failureTargetRole",
            "failureTargetPosition",
        ):
            self.assertIn(f'fields.put("{field}"', terminal_failure)

    def test_terminal_required_schema_is_stable_for_absent_snapshots(self):
        required_root = FABRIC_PROJECTION_ROOT / "terminal" / "payload" / "required"
        mismatch_text = (
            required_root
            / "FabricChatClefCraftResourceTerminalMismatchRequiredFields.java"
        ).read_text(encoding="utf-8")
        failure_text = (
            required_root
            / "target"
            / "FabricChatClefCraftResourceTerminalFailureRequiredFields.java"
        ).read_text(encoding="utf-8")
        optional_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "optional"
            / "FabricChatClefCraftResourceTerminalOptionalFields.java"
        ).read_text(encoding="utf-8")

        unavailable_mismatch_counters = (
            "commandMismatchUnownedObservationCount",
            "commandMismatchUnknownAssociationCount",
        )
        for field in unavailable_mismatch_counters:
            self.assertEqual(2, mismatch_text.count(f'fields.put("{field}"'))
            self.assertIn(
                f'fields.put("{field}", "UNAVAILABLE_SCOPE_NOT_ACTIVE")',
                mismatch_text,
            )
        self.assertEqual(
            2,
            mismatch_text.count('fields.put("commandMismatchCounterSaturated"'),
        )
        self.assertIn(
            'fields.put("commandMismatchCounterSaturated", false)',
            mismatch_text,
        )

        self.assertEqual(
            2,
            failure_text.count('fields.put("failureAssociationStatus"'),
        )
        self.assertIn(
            'fields.put("failureAssociationStatus", '
            '"UNAVAILABLE_FAILURE_SCOPE_NOT_ACTIVE")',
            failure_text,
        )
        self.assertIn(
            'fields.put("failureAssociationStatus", '
            'failure.associationStatus().name())',
            failure_text,
        )
        self.assertNotIn('fields.put("failureAssociationStatus"', optional_text)
        self.assertIn(
            'fields.put("failureTargetAttemptSequence", '
            'unavailableLong(failure.targetAttemptSequence()))',
            failure_text,
        )
        self.assertNotIn(
            'fields.put("failureTargetAttemptSequence", '
            'failure.targetAttemptSequence())',
            failure_text,
        )

    def test_terminal_payload_preserves_contributor_insertion_order(self):
        payload_text = (
            FABRIC_PROJECTION_ROOT
            / "terminal"
            / "payload"
            / "FabricChatClefCraftResourceTerminalPayload.java"
        ).read_text(encoding="utf-8")

        self.assertIn("import java.util.Collections;", payload_text)
        self.assertIn("import java.util.LinkedHashMap;", payload_text)
        self.assertEqual(2, payload_text.count("Collections.unmodifiableMap("))
        self.assertEqual(2, payload_text.count("new LinkedHashMap<>("))
        self.assertNotIn("Map.copyOf(", payload_text)


if __name__ == "__main__":
    unittest.main()
