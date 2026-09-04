package lavi.minecraft.diagnostics.container.gui;

import adris.altoclef.eventbus.events.ScreenOpenEvent;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.gui.dispatch.ContainerScreenDispatchProbe;
import lavi.minecraft.diagnostics.container.gui.runtime.ContainerGuiDiagnosticRuntime;
import lavi.minecraft.diagnostics.container.gui.slot.ContainerSlotActionProbe;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

//20260904_kpopmodder: Expose nonthrowing observation calls without mixing formatting or gameplay behavior.
public final class ContainerGuiDiagnostics {
    private static final ContainerGuiDiagnosticRuntime RUNTIME =
            new ContainerGuiDiagnosticRuntime();

    private ContainerGuiDiagnostics() {
    }

    public static BlockInteractionObserver interactionObserver() {
        return RUNTIME.interactionObserver();
    }

    public static DiagnosticSessionLifecycleObserver lifecycleObserver() {
        return RUNTIME;
    }

    public static boolean screenTransportObservationEnabled() {
        try {
            return ChatClefDiagnostics.isBoundaryEnabled();
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static void onClientTickHead(long serial) {
        run(() -> RUNTIME.onClientTickHead(serial));
    }

    public static void onClientTickBoundaryPublished(long serial) {
        run(() -> RUNTIME.onClientTickBoundaryPublished(serial));
    }

    public static void onClientTickReturn(long serial) {
        run(() -> RUNTIME.onClientTickReturn(serial));
    }

    public static void onScreenTailSource(ScreenOpenEvent event) {
        run(() -> RUNTIME.onScreenTailSource(event));
    }

    public static ContainerScreenDispatchProbe beginScreenDispatch(
            ScreenOpenEvent event,
            int registeredListenerCount,
            int eligibleListenerCount) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || event == null || event.preOpen) {
            return ContainerScreenDispatchProbe.noop();
        }
        try {
            return RUNTIME.beginScreenDispatch(
                    event,
                    registeredListenerCount,
                    eligibleListenerCount
            );
        } catch (RuntimeException | LinkageError ignored) {
            return ContainerScreenDispatchProbe.noop();
        }
    }

    public static void onTaskReconciliation(
            Task parent,
            Task activeChildBefore,
            Task candidateChild,
            boolean isEqualResult,
            boolean canInterruptEvaluated,
            boolean canInterruptPreviousChild,
            boolean replacementApplied,
            boolean previousChildStopCalled,
            Task activeChildAfter,
            boolean candidateDiscardedBecauseEqual,
            boolean childCleared) {
        run(() -> RUNTIME.onTaskReconciliation(
                parent,
                activeChildBefore,
                candidateChild,
                isEqualResult,
                canInterruptEvaluated,
                canInterruptPreviousChild,
                replacementApplied,
                previousChildStopCalled,
                activeChildAfter,
                candidateDiscardedBecauseEqual,
                childCleared
        ));
    }

    public static void onTaskEvaluationStarted(Task task) {
        run(() -> RUNTIME.onTaskEvaluationStarted(task));
    }

    public static ContainerSlotActionProbe beginSlotAction(
            ScreenHandler handler,
            int syncId,
            int windowSlot,
            int button,
            SlotActionType actionType,
            ClientPlayerEntity player) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return ContainerSlotActionProbe.noop();
        }
        try {
            return RUNTIME.beginSlotAction(
                    handler,
                    syncId,
                    windowSlot,
                    button,
                    actionType,
                    player
            );
        } catch (RuntimeException | LinkageError ignored) {
            return ContainerSlotActionProbe.noop();
        }
    }

    public static void onLocalSlotMutation(
            ScreenHandler handler,
            int windowSlot,
            ItemStack before,
            ItemStack after) {
        run(() -> RUNTIME.onLocalSlotMutation(handler, windowSlot, before, after));
    }

    public static void onServerSlotUpdateApplied(
            ScreenHandler handler,
            int syncId,
            int revision,
            int windowSlot,
            ItemStack packetStack) {
        run(() -> RUNTIME.onServerSlotUpdateApplied(
                handler,
                syncId,
                revision,
                windowSlot,
                packetStack
        ));
    }

    public static void onServerInventoryUpdateApplied(
            ScreenHandler handler,
            int syncId,
            int revision,
            int packetSlotCount,
            ItemStack packetCursorStack) {
        run(() -> RUNTIME.onServerInventoryUpdateApplied(
                handler,
                syncId,
                revision,
                packetSlotCount,
                packetCursorStack
        ));
    }

    public static void onUserTaskTerminal(
            Task task,
            boolean actuallyDone,
            String frozenRootAssignmentId,
            String currentRootAssignmentId,
            String finishTriggerHint) {
        run(() -> RUNTIME.onUserTaskTerminal(
                task,
                actuallyDone,
                frozenRootAssignmentId,
                currentRootAssignmentId,
                finishTriggerHint
        ));
    }

    private static void run(Runnable action) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
