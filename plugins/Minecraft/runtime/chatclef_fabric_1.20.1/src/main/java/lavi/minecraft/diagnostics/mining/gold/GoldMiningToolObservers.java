package lavi.minecraft.diagnostics.mining.gold;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.ObservationDiagnostics;
import lavi.minecraft.integration.mining.operation.MiningOperationToolState;
import lavi.minecraft.integration.mining.operation.MiningToolCandidate;
import lavi.minecraft.integration.mining.operation.MiningToolRole;
import lavi.minecraft.integration.mining.operation.PrepareThenMineRawGoldTask;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;

import java.util.function.Consumer;

//20260913_kpopmodder: Route passive gold observations to the actual parent without a global task registry.
public final class GoldMiningToolObservers {
    private GoldMiningToolObservers() { }

    public static void preparation(AltoClef mod, PrepareThenMineRawGoldTask parent,
                                   MiningOperationToolState state, Task selectedChild) {
        observe(() -> {
            GoldToolLoopObserver observer = parent.diagnosticToolLoopObserver();
            if (observer.bind(mod, parent)) observer.preparation(mod, parent, state, selectedChild);
        });
    }

    public static void policyEvaluated(AltoClef mod, MiningToolFilterTrace target, MiningToolFilterTrace access) {
        withParent(mod, null, observer -> observer.filters(target.snapshot(), access.snapshot()));
    }

    public static void parentStopped(AltoClef mod, PrepareThenMineRawGoldTask parent, Task interrupter) {
        observe(() -> {
            GoldToolLoopObserver observer = parent.diagnosticToolLoopObserver();
            observer.parentStopped(interrupter);
        });
    }

    public static void equip(AltoClef mod, long attempt, String reason, ItemStack displaced, Object... fields) {
        withParent(mod, null, observer -> {
            Object[] observed = new Object[fields.length + 2];
            System.arraycopy(fields, 0, observed, 0, fields.length);
            observed[fields.length] = "displacedItemLocationsAfterEquip";
            observed[fields.length + 1] = GoldToolSnapshot.displacedItemLocations(mod, displaced);
            observer.equip(attempt, reason, observed);
        });
    }

    public static void selection(AltoClef mod, long attempt, BlockPos target, Object... fields) {
        withParent(mod, null, observer -> observer.selection(attempt, ChatClefDiagnostics.blockPos(target), fields));
    }

    public static void childStarted(AltoClef mod, Task child, BlockPos target) {
        withParent(mod, child, observer -> observer.childStarted(child, ChatClefDiagnostics.blockPos(target)));
    }

    public static void childStopped(AltoClef mod, Task child, BlockPos target, Task interrupter) {
        withParent(mod, child, observer -> observer.childStopped(child, interrupter, ChatClefDiagnostics.blockPos(target),
                "playerPosition", ChatClefDiagnostics.safeValue(() -> mod.getPlayer().getPos()),
                "breakingPosition", ChatClefDiagnostics.safeValue(() -> mod.getControllerExtras().getBreakingBlockPos()),
                "breakingProgress", ChatClefDiagnostics.safeValue(() -> mod.getControllerExtras().getBreakingBlockProgress()),
                "progressAttribution", "CURRENT_BREAKING_POSITION_ONLY_NOT_DROP_PICKUP_PROOF"));
    }

    public static GoldHotbarMoveObservation hotbarBefore(AltoClef mod, Task child,
                                                         MiningToolCandidate source, MiningToolRole role, int destination) {
        GoldHotbarMoveObservation[] captured = {null};
        withParent(mod, child, observer -> captured[0] = new GoldHotbarMoveObservation(observer, mod, source, role, destination));
        return captured[0];
    }

    public static void hotbarAfter(AltoClef mod, GoldHotbarMoveObservation captured, boolean normalReturn) {
        if (captured != null) observe(() -> captured.returned(mod, normalReturn));
    }

    private static void withParent(AltoClef mod, Task descendant, Consumer<GoldToolLoopObserver> action) {
        observe(() -> {
            if (mod == null || mod.getUserTaskChain() == null) return;
            Task root = mod.getUserTaskChain().getCurrentTask();
            if (root == null) return;
            PrepareThenMineRawGoldTask[] found = {null};
            root.thisOrChildSatisfies(task -> {
                if (task instanceof PrepareThenMineRawGoldTask parent
                        && (descendant == null || parent.thisOrChildSatisfies(child -> child == descendant))) {
                    found[0] = parent;
                    return true;
                }
                return false;
            });
            if (found[0] != null) {
                GoldToolLoopObserver observer = found[0].diagnosticToolLoopObserver();
                if (observer.bind(mod, found[0])) action.accept(observer);
            }
        });
    }

    private static void observe(Runnable action) {
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> {
            try { action.run(); }
            catch (RuntimeException | LinkageError failure) {
                // Observation failure never changes tool, inventory, task, or defensive behavior.
                ObservationDiagnostics.captureFailed("mining", failure.getClass().getSimpleName());
            }
        });
    }
}
