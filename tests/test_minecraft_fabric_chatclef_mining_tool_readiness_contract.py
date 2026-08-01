#20260801_kpopmodder: Guard target-aware mining tool acquisition without weakening saved-tool policy.
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


class MinecraftFabricChatClefMiningToolReadinessContractTests(unittest.TestCase):
    def test_mining_tool_readiness_lives_in_lavi_owned_package(self):
        text = (
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "integration"
            / "mining"
            / "MiningToolReadiness.java"
        ).read_text(encoding="utf-8")

        self.assertIn("package lavi.minecraft.integration.mining;", text)
        self.assertIn("public static Readiness evaluate(", text)
        self.assertIn("StorageHelper.getBestToolSlot(mod, targetState).isPresent()", text)
        self.assertIn("StorageHelper.shouldSaveStack(mod, targetState.getBlock(), stack)", text)
        self.assertIn("requirement != MiningRequirement.HAND", text)
        self.assertIn("broadRequirementMet", text)
        self.assertIn("selectableToolPresent", text)
        self.assertIn("rejectedBySavePolicy", text)
        self.assertIn("requiresAcquisition", text)
        self.assertNotIn("TaskRunner", text)
        self.assertNotIn("CommandExecutor", text)
        self.assertNotIn("DestroyBlockTask", text)
        self.assertNotIn("forceCancel", text)
        self.assertNotIn("Thread.sleep", text)
        self.assertNotIn("latest.log", text)

    def test_mine_and_collect_acquires_target_eligible_tool_before_destroy_task(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "resources"
            / "MineAndCollectTask.java"
        ).read_text(encoding="utf-8")

        self.assertIn("import lavi.minecraft.integration.mining.MiningToolReadiness;", text)
        self.assertIn("_subtask = new MineOrCollectTask(_blocksToMine, this.itemTargets, _requirement);", text)
        self.assertIn("private final MiningRequirement _requirement;", text)
        self.assertIn("public MineOrCollectTask(Block[] blocks, ItemTarget[] targets, MiningRequirement requirement)", text)
        self.assertIn("MiningToolReadiness.evaluate(mod, targetState, _requirement)", text)
        self.assertIn("if (readiness.requiresAcquisition())", text)
        self.assertIn("miningPos = null;", text)
        self.assertIn("progressChecker.reset();", text)
        self.assertIn("new SatisfyMiningRequirementTask(_requirement, targetState)", text)
        self.assertIn("mine_or_collect_return_target_tool_requirement_task", text)
        self.assertIn("&& task._requirement == _requirement", text)

        acquire_index = text.index("if (readiness.requiresAcquisition())")
        reset_index = text.index("miningPos = null;", acquire_index)
        requirement_index = text.index("new SatisfyMiningRequirementTask(_requirement, targetState)", acquire_index)
        destroy_index = text.index("Task destroyTask = new DestroyBlockTask(miningPos);", acquire_index)
        assign_index = text.index("miningPos = newPos;", acquire_index)

        self.assertLess(acquire_index, reset_index)
        self.assertLess(reset_index, requirement_index)
        self.assertLess(requirement_index, assign_index)
        self.assertLess(assign_index, destroy_index)

    def test_satisfy_mining_requirement_preserves_legacy_mode_and_adds_target_mode(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "resources"
            / "SatisfyMiningRequirementTask.java"
        ).read_text(encoding="utf-8")

        self.assertIn("private final BlockState targetState;", text)
        self.assertIn("this(requirement, null);", text)
        self.assertIn("public SatisfyMiningRequirementTask(MiningRequirement requirement, BlockState targetState)", text)
        self.assertIn("TaskCatalogue.getItemTask(Items.WOODEN_PICKAXE, 1)", text)
        self.assertIn("TaskCatalogue.getItemTask(Items.STONE_PICKAXE, 1)", text)
        self.assertIn("TaskCatalogue.getItemTask(Items.IRON_PICKAXE, 1)", text)
        self.assertIn("TaskCatalogue.getItemTask(Items.DIAMOND_PICKAXE, 1)", text)
        self.assertIn("Objects.equals(task.targetState, targetState)", text)
        self.assertIn("if (targetState != null)", text)
        self.assertIn("MiningToolReadiness.hasSelectableMiningTool(AltoClef.getInstance(), targetState)", text)
        self.assertIn("return StorageHelper.miningRequirementMetInventory(requirement);", text)

    def test_saved_iron_pickaxe_policy_is_not_weakened(self):
        text = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "util"
            / "helpers"
            / "StorageHelper.java"
        ).read_text(encoding="utf-8")

        self.assertIn("public static boolean shouldSaveStack", text)
        self.assertIn("!stack.getItem().equals(Items.IRON_PICKAXE)", text)
        self.assertIn("mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE)", text)
        self.assertIn("if (stack.getDamage()+30 > stack.getMaxDamage())", text)
        self.assertIn("return !MiningRequirement.getMinimumRequirementForBlock(block).equals(MiningRequirement.IRON);", text)
        self.assertNotIn("USE_LAST_ELIGIBLE_TOOL", text)
        self.assertNotIn("NEVER_MINE_BY_HAND", text)

    def test_patch_does_not_move_crafting_into_destroy_or_bridge_boundaries(self):
        forbidden_files = (
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "tasks"
            / "construction"
            / "DestroyBlockTask.java",
            JAVA_ROOT
            / "adris"
            / "altoclef"
            / "chains"
            / "PlayerInteractionFixChain.java",
            JAVA_ROOT
            / "lavi"
            / "minecraft"
            / "fabric"
            / "chatclef"
            / "bridge"
            / "command"
            / "FabricChatClefCommandResult.java",
        )

        for path in forbidden_files:
            text = path.read_text(encoding="utf-8")
            self.assertNotIn("MiningToolReadiness", text, str(path))
            self.assertNotIn("SatisfyMiningRequirementTask(_requirement, targetState)", text, str(path))


if __name__ == "__main__":
    unittest.main()
