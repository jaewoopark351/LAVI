package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.operation.MiningOperationToolDiagnostics;
import net.minecraft.item.Items;
//#if MC == 12001
import lavi.minecraft.integration.mining.operation.hotbar.lifecycle.MiningHotbarOperation;
//#endif

public final class PrepareMiningOperationToolsTask extends Task {
    private final MiningOperationToolPolicy policy;
    //#if MC == 12001
    //20260913_kpopmodder: Receive actual parent-owned placement state instead of resetting it with each child.
    private final MiningHotbarOperation hotbarOperation;
    public PrepareMiningOperationToolsTask(MiningOperationToolPolicy policy, MiningHotbarOperation operation) {
        this.policy = policy; this.hotbarOperation = operation;
    }
    //#endif

    public PrepareMiningOperationToolsTask(MiningOperationToolPolicy policy) {
        this.policy = policy;
        //#if MC == 12001
        this.hotbarOperation = new MiningHotbarOperation(policy);
        //#endif
    }

    @Override
    protected void onStart() {
        //#if MC == 12001
        hotbarOperation.bind(AltoClef.getInstance());
        //#endif
        MiningOperationToolDiagnostics.logDecision(this, "prepare_mining_operation_tools_start", policy.evaluate(AltoClef.getInstance()));
    }

    @Override
    protected Task onTick() {
        MiningOperationToolState state = policy.evaluate(AltoClef.getInstance());
        MiningOperationToolDiagnostics.logDecision(this, "prepare_mining_operation_tools_tick", state);

        return switch (state.nextStep()) {
            case ACQUIRE_TARGET_PICKAXE -> TaskCatalogue.getItemTask(Items.IRON_PICKAXE, state.requiredIronPickaxeCount());
            //#if MC == 12001
            case MOVE_TARGET_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.TARGET, hotbarOperation);
            //#else
            //$$ case MOVE_TARGET_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.TARGET);
            //#endif
            case ACQUIRE_ACCESS_PICKAXE -> TaskCatalogue.getItemTask(Items.STONE_PICKAXE, state.requiredStonePickaxeCount());
            //#if MC == 12001
            case MOVE_ACCESS_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.ACCESS, hotbarOperation);
            //#else
            //$$ case MOVE_ACCESS_PICKAXE_TO_HOTBAR -> new MoveMiningToolToHotbarTask(policy, MiningToolRole.ACCESS);
            //#endif
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
        //#if MC == 12001
        if (hotbarOperation.hasPending()) return false;
        //#endif
        return policy.evaluate(AltoClef.getInstance()).ready();
    }
}
