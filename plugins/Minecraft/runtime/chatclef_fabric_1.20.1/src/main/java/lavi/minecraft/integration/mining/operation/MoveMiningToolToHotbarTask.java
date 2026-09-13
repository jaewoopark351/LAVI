package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.operation.MiningOperationToolDiagnostics;
import lavi.minecraft.diagnostics.mining.gold.GoldHotbarMoveObservation;
import lavi.minecraft.diagnostics.mining.gold.GoldMiningToolObservers;

import java.util.Optional;
//#if MC == 12001
import lavi.minecraft.integration.mining.operation.hotbar.lifecycle.MiningHotbarOperation;
//#endif

public final class MoveMiningToolToHotbarTask extends Task {
    private final MiningOperationToolPolicy policy;
    private final MiningToolRole role;
    //#if MC == 12001
    //20260913_kpopmodder: Keep SWAP confirmation independent of the selected hand and child interruption.
    private final MiningHotbarOperation hotbarOperation;
    public MoveMiningToolToHotbarTask(MiningOperationToolPolicy policy, MiningToolRole role, MiningHotbarOperation operation) {
        this.policy = policy; this.role = role; this.hotbarOperation = operation;
    }
    //#endif

    public MoveMiningToolToHotbarTask(MiningOperationToolPolicy policy, MiningToolRole role) {
        this.policy = policy;
        this.role = role;
        //#if MC == 12001
        this.hotbarOperation = new MiningHotbarOperation(policy);
        //#endif
    }

    @Override
    protected void onStart() {
        //#if MC == 12001
        hotbarOperation.bind(AltoClef.getInstance());
        //#endif
        MiningOperationToolDiagnostics.logDecision(this, "move_mining_tool_to_hotbar_start", policy.evaluate(AltoClef.getInstance()),
                "toolRole", role);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        MiningOperationToolState state = policy.evaluate(mod);
        MiningOperationToolDiagnostics.logDecision(this, "move_mining_tool_to_hotbar_tick", state,
                "toolRole", role);
        //#if MC == 12001
        hotbarOperation.advance(mod, role, state, this);
        return null;
        //#else
        //$$ if (state.hotbarVisibleFor(role)) {
        //$$     return null;
        //$$ }
        //$$ Optional<MiningToolCandidate> source = state.candidateFor(role);
        //$$ if (source.isEmpty() || mod.getPlayer() == null) {
        //$$     return null;
        //$$ }
        //$$ int destinationHotbarSlot = state.preferredHotbarSlotFor(role);
        //$$ MiningOperationToolDiagnostics.logHotbarMove(this, "move_mining_tool_to_hotbar_force_equip_slot",
        //$$         role, state, source.get(), destinationHotbarSlot);
        //$$ //20260913_kpopmodder: Observe the original void call and local slot reflection without interpreting a success result.
        //$$ GoldHotbarMoveObservation observation = GoldMiningToolObservers.hotbarBefore(mod, this, source.get(), role, destinationHotbarSlot);
        //$$ boolean normalReturn = false;
        //$$ try {
        //$$     mod.getPlayer().getInventory().selectedSlot = destinationHotbarSlot;
        //$$     mod.getSlotHandler().forceEquipSlot(source.get().slot());
        //$$     normalReturn = true;
        //$$ } finally {
        //$$     GoldMiningToolObservers.hotbarAfter(mod, observation, normalReturn);
        //$$ }
        //$$ return null;
        //#endif
    }

    @Override
    protected void onStop(Task interruptTask) {
        MiningOperationToolDiagnostics.logDecision(this, "move_mining_tool_to_hotbar_stop", policy.evaluate(AltoClef.getInstance()),
                "toolRole", role,
                "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask));
    }

    @Override
    protected boolean isEqual(Task other) {
        return other instanceof MoveMiningToolToHotbarTask task
                && task.role == role
                && task.policy.isSameOperation(policy);
    }

    @Override
    protected String toDebugString() {
        return "Move " + role + " mining tool to hotbar";
    }

    @Override
    public boolean isFinished() {
        //#if MC == 12001
        if (hotbarOperation.hasPending(role)) return false;
        //#endif
        return policy.evaluate(AltoClef.getInstance()).hotbarVisibleFor(role);
    }
}
