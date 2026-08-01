#20260801_kpopmodder: Guard bounded task-stack diagnostics for the visible mining/furnace chain.
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


class MinecraftFabricChatClefTaskTraceDiagnosticsContractTests(unittest.TestCase):
    def test_visible_task_diagnostics_live_in_lavi_owned_package(self):
        diagnostics_file = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "tasktrace"
            / "VisibleTaskDiagnostics.java"
        )

        self.assertTrue(diagnostics_file.exists())
        text = diagnostics_file.read_text(encoding="utf-8")

        self.assertIn("package lavi.minecraft.diagnostics.tasktrace;", text)
        self.assertIn("logBoundary(\"VISIBLE_TASK_LIFECYCLE\"", text)
        self.assertIn("logBoundary(eventName", text)
        self.assertIn("WeakHashMap", text)
        self.assertIn("markStateChanged", text)
        self.assertIn("MAX_STATE_KEY_LENGTH", text)
        self.assertNotIn("latest.log", text)

    def test_visible_task_diagnostics_do_not_change_lifecycle_or_input_ownership(self):
        text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "tasktrace"
            / "VisibleTaskDiagnostics.java"
        ).read_text(encoding="utf-8")

        forbidden_fragments = (
            "TaskRunner",
            "CommandExecutor",
            "UserTaskChain",
            "SingleTaskChain",
            "forceCancel",
            "setGoalAndPath",
            "setInputForceState",
            "hold(",
            "release(",
            "Thread.sleep",
        )

        for fragment in forbidden_fragments:
            self.assertNotIn(fragment, text, fragment)

    def test_visible_stack_tasks_call_bounded_tasktrace_helper(self):
        files = {
            "SmeltInFurnaceTask.java": JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "container"
            / "SmeltInFurnaceTask.java",
            "CollectFuelTask.java": JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "resources"
            / "CollectFuelTask.java",
            "MineAndCollectTask.java": JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "resources"
            / "MineAndCollectTask.java",
            "DestroyBlockTask.java": JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "construction"
            / "DestroyBlockTask.java",
        }

        for name, path in files.items():
            text = path.read_text(encoding="utf-8")
            self.assertIn(
                "import lavi.minecraft.diagnostics.tasktrace.VisibleTaskDiagnostics;",
                text,
                name,
            )
            self.assertIn("VisibleTaskDiagnostics.", text, name)

        mine_text = files["MineAndCollectTask.java"].read_text(encoding="utf-8")
        self.assertIn("mine_or_collect_return_destroy_block_task", mine_text)
        self.assertIn("mine_or_collect_closest_choice", mine_text)

        destroy_text = files["DestroyBlockTask.java"].read_text(encoding="utf-8")
        self.assertIn("destroy_block_in_range_mining", destroy_text)
        self.assertIn("destroy_block_set_goal_and_path", destroy_text)
        self.assertIn("destroy_block_is_finished", destroy_text)

    def test_tasktrace_uses_boundary_mode_not_verbose_only_events(self):
        tasktrace_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "tasktrace"
            / "VisibleTaskDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ChatClefDiagnostics.isBoundaryEnabled()", tasktrace_text)
        self.assertNotIn("ChatClefDiagnostics.logEvent", tasktrace_text)


if __name__ == "__main__":
    unittest.main()
