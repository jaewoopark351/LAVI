package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.operation.MiningOperationToolDiagnostics;

import java.util.Optional;

public final class MoveMiningToolToHotbarTask extends Task {
    private final MiningOperationToolPolicy policy;
    private final MiningToolRole role;

    public MoveMiningToolToHotbarTask(MiningOperationToolPolicy policy, MiningToolRole role) {
        this.policy = policy;
        this.role = role;
    }

    @Override
    protected void onStart() {
        MiningOperationToolDiagnostics.logDecision(this, "move_mining_tool_to_hotbar_start", policy.evaluate(AltoClef.getInstance()),
                "toolRole", role);
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();
        MiningOperationToolState state = policy.evaluate(mod);
        MiningOperationToolDiagnostics.logDecision(this, "move_mining_tool_to_hotbar_tick", state,
                "toolRole", role);
        if (state.hotbarVisibleFor(role)) {
            return null;
        }

        Optional<MiningToolCandidate> source = state.candidateFor(role);
        if (source.isEmpty() || mod.getPlayer() == null) {
            return null;
        }

        int destinationHotbarSlot = state.preferredHotbarSlotFor(role);
        MiningOperationToolDiagnostics.logHotbarMove(
                this,
                "move_mining_tool_to_hotbar_force_equip_slot",
                role,
                state,
                source.get(),
                destinationHotbarSlot
        );
        mod.getPlayer().getInventory().selectedSlot = destinationHotbarSlot;
        mod.getSlotHandler().forceEquipSlot(source.get().slot());
        return null;
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
        return policy.evaluate(AltoClef.getInstance()).hotbarVisibleFor(role);
    }
}
