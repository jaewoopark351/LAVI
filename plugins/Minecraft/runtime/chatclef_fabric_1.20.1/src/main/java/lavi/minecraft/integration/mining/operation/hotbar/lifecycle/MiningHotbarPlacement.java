//#if MC == 12001
package lavi.minecraft.integration.mining.operation.hotbar.lifecycle;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.integration.mining.operation.*;
import lavi.minecraft.integration.mining.operation.hotbar.layout.MiningHotbarContext;
import lavi.minecraft.integration.toolselect.equip.diagnostics.ToolEquipAttemptDiagnostics;
import lavi.minecraft.integration.toolselect.equip.execution.*;
import lavi.minecraft.integration.toolselect.equip.model.*;
import lavi.minecraft.integration.toolselect.equip.validation.ToolEquipRequestFactory;
import java.util.EnumMap;
import java.util.Optional;

//20260913_kpopmodder: Coordinate role placement attempts while lifecycle and root termination remain elsewhere.
public final class MiningHotbarPlacement {
    private final MiningOperationToolPolicy policy;
    private final EnumMap<MiningToolRole, ToolEquipAttempt> attempts = new EnumMap<>(MiningToolRole.class);
    private final MiningHotbarProgress progress = new MiningHotbarProgress();
    private final ToolEquipAttemptDiagnostics diagnostics = new ToolEquipAttemptDiagnostics();
    public MiningHotbarPlacement(MiningOperationToolPolicy policy) { this.policy = java.util.Objects.requireNonNull(policy); }
    public void advance(AltoClef mod, MiningToolRole role, MiningOperationToolState state, Task owner) {
        if (progress.failure().isPresent()) return;
        ToolEquipAttempt attempt = attempts.get(role);
        if (attempt == null && state.hotbarVisibleFor(role)) return;
        var candidate = state.candidateFor(role);
        if (candidate.isEmpty() && attempt == null) return;
        int source = attempt == null ? candidate.orElseThrow().slot().getInventorySlot() : attempt.request().sourceInventory();
        MinecraftToolEquipPort port = new MinecraftToolEquipPort(mod,
                stack -> policy.evaluate(mod).candidateFor(role).map(value -> value.stack()).filter(value -> net.minecraft.item.ItemStack.areEqual(value, stack)).isPresent(),
                destination -> MiningHotbarContext.available(mod, source, destination, policy.evaluate(mod)));
        if (!port.inputAvailable()) return;
        if (attempt == null) {
            int destination = MiningHotbarContext.destination(mod, source, state);
            if (destination < 0) { blocked(owner, "HOTBAR_LAYOUT_UNAVAILABLE"); return; }
            var selected = candidate.orElseThrow();
            attempt = new ToolEquipAttempt(ToolEquipRequestFactory.capture(port, ToolEquipPurpose.HOTBAR_PLACEMENT,
                    source, selected.slot().getWindowSlot(), MinecraftToolEquipPort.value(selected.stack()), destination));
            attempts.put(role, attempt);
        }
        ToolEquipStatus status = attempt.advance(port);
        diagnostics.result(owner, attempt, role.name());
        if (status.success()) { attempts.remove(role); progress.confirmedProgress(); }
        else if (status.terminal()) {
            if (attempt.swapCount() != 0) progress.confirmationFailed();
            else { attempts.remove(role); blocked(owner, "HOTBAR_LAYOUT_UNAVAILABLE"); }
        }
    }
    public void exactEquipFailed(Task owner, ToolEquipAttempt attempt) {
        if (attempt.swapCount() != 0) progress.confirmationFailed();
        else blocked(owner, "HOTBAR_LAYOUT_UNAVAILABLE");
    }
    public void pollPending(AltoClef mod, MiningOperationToolState state, Task owner) {
        // Two role keys only; a parent may observe ready before its move child next evaluates.
        for (MiningToolRole role : MiningToolRole.values()) if (attempts.containsKey(role)) advance(mod, role, state, owner);
    }
    public boolean hasPending() { return !attempts.isEmpty() && progress.failure().isEmpty(); }
    public boolean hasPending(MiningToolRole role) { return attempts.containsKey(role) && progress.failure().isEmpty(); }
    private void blocked(Task owner, String reason) {
        progress.blocked(reason);
        progress.failure().ifPresent(value -> diagnostics.failure(owner, value, progress.blockedEvaluations()));
    }
    public Optional<String> failureReason() { return progress.failure(); }
    public void reportFailure(Task owner) {
        progress.failure().ifPresent(reason -> diagnostics.failure(owner, reason, progress.blockedEvaluations()));
    }
}
//#endif
