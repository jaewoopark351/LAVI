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
        self.assertIn("logBoundaryWithPhysicalOutcome(", text)
        self.assertIn("boolean sourceEmissionCompleted", text)
        self.assertIn(
            'sourceEmissionCompleted && "VISIBLE_TASK_RETURN".equals(eventName)',
            text,
        )
        self.assertIn(
            "CraftResourceRequirementSourceEventObserver.observeVisibleTaskReturn(task)",
            text,
        )
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
        mining_facade_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "MiningPathDiagnostics.java"
        ).read_text(encoding="utf-8")
        mine_goal_diagnostics_text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "diagnostics"
            / "mining"
            / "MineTargetGoalRequestDiagnostics.java"
        ).read_text(encoding="utf-8")

        self.assertIn(
            "import lavi.minecraft.diagnostics.mining.MiningPathDiagnostics;",
            mine_text,
        )
        destroy_task_index = mine_text.index(
            "Task destroyTask = new DestroyBlockTask(miningPos);"
        )
        goal_request_index = mine_text.index(
            "MiningPathDiagnostics.logMineTargetGoalRequest(", destroy_task_index
        )
        outcome_index = mine_text.index(
            '"RETURN_DESTROY_BLOCK_TASK", destroyTask);', goal_request_index
        )
        return_index = mine_text.index("return destroyTask;", outcome_index)
        self.assertLess(destroy_task_index, goal_request_index)
        self.assertLess(goal_request_index, outcome_index)
        self.assertLess(outcome_index, return_index)
        self.assertEqual(
            1, mine_text.count('"RETURN_DESTROY_BLOCK_TASK", destroyTask);')
        )
        self.assertNotIn("mine_or_collect_return_destroy_block_task", mine_text)
        self.assertIn(
            "public static void logMineTargetGoalRequest", mining_facade_text
        )
        self.assertIn(
            "readiness, decisionOutcome, returnedTask);", mining_facade_text
        )
        self.assertIn("return MiningDiagnosticEmitter.emitLazyWithPhysicalOutcome(",
                      mine_goal_diagnostics_text)
        source_outcome_index = mining_facade_text.index(
            "boolean sourceEmissionCompleted = MineTargetGoalRequestDiagnostics.log("
        )
        projection_index = mining_facade_text.index(
            "MiningProjectionObserverRegistry.observeMineTargetGoalRequest(",
            source_outcome_index,
        )
        projection_outcome_index = mining_facade_text.index(
            "sourceEmissionCompleted);",
            projection_index,
        )
        self.assertLess(source_outcome_index, projection_index)
        self.assertLess(projection_index, projection_outcome_index)
        self.assertIn(
            '"returnedTaskInstanceId", '
            "MiningDiagnosticEmitter.instanceId(returnedTask)",
            mine_goal_diagnostics_text,
        )
        self.assertIn("mine_or_collect_closest_choice", mine_text)

        destroy_text = files["DestroyBlockTask.java"].read_text(encoding="utf-8")
        self.assertIn("destroy_block_in_range_mining", destroy_text)
        self.assertIn("destroy_block_set_goal_and_path", destroy_text)
        self.assertNotIn("destroy_block_is_finished", destroy_text)
        finish_method_index = destroy_text.index("public boolean isFinished()")
        finish_state_index = destroy_text.index(
            "BlockState blockState = mod.getWorld().getBlockState(pos);",
            finish_method_index,
        )
        finish_result_index = destroy_text.index(
            "boolean isAir = blockState.isAir();", finish_state_index
        )
        finish_diagnostics_index = destroy_text.index(
            "MiningPathDiagnostics.logDestroyFinishEvaluation("
            "mod, this, pos, blockState, isAir);",
            finish_result_index,
        )
        finish_return_index = destroy_text.index(
            "return isAir;", finish_diagnostics_index
        )
        self.assertLess(finish_state_index, finish_result_index)
        self.assertLess(finish_result_index, finish_diagnostics_index)
        self.assertLess(finish_diagnostics_index, finish_return_index)
        self.assertIn(
            "DestroyFinishEvaluationDiagnostics.log(",
            mining_facade_text,
        )
        self.assertIn(
            "mod, task, target, observedBlockState, observedIsAir);",
            mining_facade_text,
        )

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
