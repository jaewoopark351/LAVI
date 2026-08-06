package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.operation.MiningOperationToolDiagnostics;
import net.minecraft.item.Items;

public final class PrepareMiningOperationToolsTask extends Task {
    private final MiningOperationToolPolicy policy;

    public PrepareMiningOperationToolsTask(MiningOperationToolPolicy policy) {
        this.policy = policy;
    }

    @Override
    protected void onStart() {
        MiningOperationToolDiagnostics.logDecision(this, "prepare_mining_operation_tools_start", policy.evaluate(AltoClef.getInstance()));
    }

    @Override
    protected Task onTick() {
        MiningOperationToolState state = policy.evaluate(AltoClef.getInstance());
        MiningOperationToolDiagnostics.logDecision(this, "prepare_mining_operation_tools_tick", state);

        return switch (state.nextStep()) {
            case ACQUIRE_TARGET_PICKAXE -> TaskCatalogue.getItemTask(Items.IRON_PICKAXE, state.requiredIronPickaxeCount());
            case MOVE_TARGET_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.TARGET);
            case ACQUIRE_ACCESS_PICKAXE -> TaskCatalogue.getItemTask(Items.STONE_PICKAXE, state.requiredStonePickaxeCount());
            case MOVE_ACCESS_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.ACCESS);
            case READY -> null;
        };
    }

    @Override
    protected void onStop(Task interruptTask) {
        MiningOperationToolDiagnostics.logDecision(this, "prepare_mining_operation_tools_stop", policy.evaluate(AltoClef.getInstance()),
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof PrepareMiningOperationToolsTask task
                && task.policy.isSameOperation(policy);
    }

    @Override
    protected String toDebugString() {
        return "Prepare Mining Operation Tools";
    }

    @Override
    public boolean isFinished() {
        return policy.evaluate(AltoClef.getInstance()).ready();
    }
}
