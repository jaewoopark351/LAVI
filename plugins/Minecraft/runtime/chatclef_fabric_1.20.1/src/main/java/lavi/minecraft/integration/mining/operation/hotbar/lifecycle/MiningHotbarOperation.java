//#if MC == 12001
package lavi.minecraft.integration.mining.operation.hotbar.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.integration.mining.operation.*;
import lavi.minecraft.integration.toolselect.equip.execution.ToolEquipAttempt;
import java.util.Optional;

//20260913_kpopmodder: Keep role attempts and failure ownership on one actual mining task lifetime.
public final class MiningHotbarOperation {
    private final MiningOperationToolPolicy policy;
    private MiningHotbarPlacement placement;
    private Object world, player, rootInvocation;
    private Task root;
    private boolean failureApplied;
    public MiningHotbarOperation(MiningOperationToolPolicy policy) {
        this.policy = java.util.Objects.requireNonNull(policy);
        this.placement = new MiningHotbarPlacement(policy);
    }
    public void bind(AltoClef mod) {
        Task currentRoot = mod.getUserTaskChain().getCurrentTask();
        if (root == currentRoot && rootInvocation == mod.getUserTaskChain().currentRootInvocation()
                && world == mod.getWorld() && player == mod.getPlayer()) return;
        root = currentRoot; world = mod.getWorld(); player = mod.getPlayer();
        rootInvocation = mod.getUserTaskChain().currentRootInvocation();
        placement = new MiningHotbarPlacement(policy); failureApplied = false;
    }
    public void advance(AltoClef mod, MiningToolRole role, MiningOperationToolState state, Task owner) {
        if (matches(mod)) placement.advance(mod, role, state, owner);
    }
    public void exactEquipFailed(Task owner, ToolEquipAttempt attempt) {
        placement.exactEquipFailed(owner, attempt);
    }
    public void pollPending(AltoClef mod, MiningOperationToolState state, Task owner) {
        if (matches(mod)) placement.pollPending(mod, state, owner);
    }
    public boolean hasPending() { return placement.hasPending(); }
    public boolean hasPending(MiningToolRole role) { return placement.hasPending(role); }
    public void failRootIfCurrent(AltoClef mod, PrepareThenMineRawGoldTask owner) {
        if (failureApplied || placement.failureReason().isEmpty() || !matches(mod)
                || mod.getTaskRunner().getCurrentTaskChain() != mod.getUserTaskChain()
                || !root.thisOrChildSatisfies(task -> task == owner)) return;
        failureApplied = true;
        String reason = placement.failureReason().orElseThrow();
        placement.reportFailure(owner);
        root.fail(reason);
    }
    public Optional<String> failureReason() { return placement.failureReason(); }
    public boolean matches(AltoClef mod) {
        return root != null && rootInvocation != null && root == mod.getUserTaskChain().getCurrentTask()
                && rootInvocation == mod.getUserTaskChain().currentRootInvocation()
                && world == mod.getWorld() && player == mod.getPlayer();
    }
}
//#endif
