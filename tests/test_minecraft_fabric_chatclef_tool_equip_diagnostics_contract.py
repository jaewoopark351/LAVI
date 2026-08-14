#20260801_kpopmodder: Guard diagnostics-only tool equip fidelity instrumentation.
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


class MinecraftFabricChatClefToolEquipDiagnosticsContractTests(unittest.TestCase):
    def test_tool_equip_diagnostics_live_in_lavi_owned_package(self):
        diagnostics_file = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "toolselect"
            / "ToolEquipDiagnostics.java"
        )

        self.assertTrue(diagnostics_file.exists())
        text = diagnostics_file.read_text(encoding="utf-8")
        formatter_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "toolselect"
            / "support"
            / "ToolDiagnosticFormatter.java"
        ).read_text(encoding="utf-8")
        save_policy_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "toolselect"
            / "support"
            / "ToolSavePolicyDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("package lavi.minecraft.diagnostics.toolselect;", text)
        self.assertIn("logBoundary(\"TOOL_SELECTION_DECISION\"", text)
        self.assertIn("logBoundary(\"TOOL_EQUIP_REQUEST\"", text)
        self.assertIn("logBoundary(\"TOOL_EQUIP_RESULT\"", text)
        self.assertIn("forceEquipReportedSuccess", text)
        self.assertIn("postconditionItemMatched", text)
        self.assertIn("postconditionExactStackMatched", text)
        self.assertIn("ToolDiagnosticFormatter.MAX_CANDIDATES", text)
        self.assertIn("MAX_CANDIDATES = 12", formatter_text)
        self.assertIn("selectionOutcome", text)
        self.assertIn("saveDecision", text)
        self.assertIn("LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED", save_policy_text)
        self.assertIn("defaultStackSuitable", text)
        self.assertNotIn("latest.log", text)

    def test_best_tool_slot_diagnostics_live_in_lavi_owned_package(self):
        diagnostics_file = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "toolselect"
            / "BestToolSlotDiagnostics.java"
        )

        self.assertTrue(diagnostics_file.exists())
        text = diagnostics_file.read_text(encoding="utf-8")
        save_policy_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "toolselect"
            / "support"
            / "ToolSavePolicyDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("package lavi.minecraft.diagnostics.toolselect;", text)
        self.assertIn("logBoundary(\"BEST_TOOL_SLOT_DECISION\"", text)
        self.assertIn("storage_helper_get_best_tool_slot", text)
        self.assertIn("private static final DiagnosticDeduplicator DEDUPLICATOR", text)
        self.assertIn("DEDUPLICATOR.shouldEmit(\"best_tool_slot\", fingerprint)", text)
        self.assertIn("private String fingerprint(Slot bestToolSlot, String decisionReason, double highestSpeed)", text)
        self.assertIn("SKIP_SHOULD_SAVE", text)
        self.assertIn("NO_ELIGIBLE_TOOL", text)
        self.assertIn("HARDNESS_ZERO_USE_EQUIP_SLOT", text)
        self.assertIn("ToolSavePolicyDiagnostics.observedDecision", text)
        self.assertIn("LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED", save_policy_text)
        self.assertIn("not_evaluated#reason=NOT_DEFAULT_SUITABLE", text)
        self.assertNotIn("latest.log", text)

    def test_storage_helper_best_tool_slot_has_diagnostics_only_hunk(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "util"
            / "helpers"
            / "StorageHelper.java"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "import lavi.minecraft.diagnostics.toolselect.BestToolSlotDiagnostics;",
            text,
        )
        self.assertIn(
            "BestToolSlotDiagnostics.Scan bestToolDiagnostics = BestToolSlotDiagnostics.start(state);",
            text,
        )
        self.assertIn(
            "bestToolDiagnostics.logReturn(equipSlot, BestToolSlotDiagnostics.DECISION_HARDNESS_ZERO_USE_EQUIP_SLOT, Double.NaN);",
            text,
        )
        self.assertIn(
            "bestToolDiagnostics.observeToolCandidate(mod, slot, stack, true, true, Double.NaN, false);",
            text,
        )
        self.assertIn(
            "? BestToolSlotDiagnostics.DECISION_NO_ELIGIBLE_TOOL",
            text,
        )
        self.assertIn(
            ": BestToolSlotDiagnostics.DECISION_SELECTED_TOOL, highestSpeed);",
            text,
        )

    def test_player_interaction_boundary_preserves_exact_best_slot_context(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "chains"
            / "PlayerInteractionFixChain.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ToolEquipDiagnostics.logSelectionDecision", text)
        self.assertIn('"NO_CANDIDATE"', text)
        self.assertIn('"ALREADY_SELECTED_SLOT"', text)
        self.assertIn('"SAME_ITEM_TYPE_SKIP"', text)
        self.assertIn('"SKIP_EATING"', text)
        self.assertIn('"SKIP_BARITONE_HOTBAR"', text)
        self.assertIn('"EQUIP_REQUEST"', text)
        self.assertIn(
            "forceEquipItem(bestToolItem, equipAttemptId, selectedBestToolSlot, selectedBestToolStack)",
            text,
        )

    def test_slot_handler_reports_result_without_changing_legacy_callers(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "control"
            / "SlotHandler.java"
        ).read_text(encoding="utf-8")

        self.assertIn("public boolean forceEquipItem(Item toEquip) {", text)
        self.assertIn("return forceEquipItem(toEquip, -1L, null, null);", text)
        self.assertIn(
            "public boolean forceEquipItem(Item toEquip, long equipAttemptId, Slot expectedSourceSlot, ItemStack expectedSourceStack)",
            text,
        )
        self.assertIn("ToolEquipDiagnostics.logEquipResult", text)
        self.assertIn('"already_equipped"', text)
        self.assertIn('"matching_slots_swapped"', text)
        self.assertIn('"missing_item"', text)

    def test_tool_equip_diagnostics_do_not_touch_global_lifecycle_or_bridge_result(self):
        touched_text = "\n".join(
            path.read_text(encoding="utf-8")
            for path in (
                JAVA_ROOT
                / "lavi"
                / "minecraft"
                / "diagnostics"
                / "toolselect"
            ).rglob("*.java")
        )

        forbidden_fragments = (
            "TaskRunner",
            "CommandExecutor",
            "UserTaskChain",
            "SingleTaskChain",
            "FabricChatClefCommandResult",
            "setInputForceState",
            "forceCancel",
            "Thread.sleep",
        )

        for fragment in forbidden_fragments:
            self.assertNotIn(fragment, touched_text, fragment)


if __name__ == "__main__":
    unittest.main()
