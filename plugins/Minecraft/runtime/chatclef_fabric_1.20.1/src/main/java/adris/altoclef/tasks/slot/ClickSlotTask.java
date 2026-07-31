package adris.altoclef.tasks.slot;

import adris.altoclef.AltoClef;
import adris.altoclef.control.SlotHandler;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.screen.slot.SlotActionType;

public class ClickSlotTask extends Task {

    private final Slot slot;
    private final int mouseButton;
    private final SlotActionType type;

    private boolean clicked = false;

    public ClickSlotTask(Slot slot, int mouseButton, SlotActionType type) {
        this.slot = slot;
        this.mouseButton = mouseButton;
        this.type = type;
    }

    public ClickSlotTask(Slot slot, SlotActionType type) {
        this(slot, 0, type);
    }

    public ClickSlotTask(Slot slot, int mouseButton) {
        this(slot, mouseButton, SlotActionType.PICKUP);
    }

    public ClickSlotTask(Slot slot) {
        this(slot, SlotActionType.PICKUP);
    }

    @Override
    protected void onStart() {
        clicked = false;
        ChatClefDiagnostics.logSlotClick("ON_START", "click_slot_task_start", slot, mouseButton, type,
                "clicked", clicked);
    }

    @Override
    protected Task onTick() {
        SlotHandler slotHandler = AltoClef.getInstance().getSlotHandler();
        ChatClefDiagnostics.logSlotClick("ON_TICK_BEGIN", "click_slot_task_tick", slot, mouseButton, type,
                "clicked", clicked);

        if (slotHandler.canDoSlotAction()) {
            ChatClefDiagnostics.logSlotClick("REQUEST", "click_slot_task_before_click", slot, mouseButton, type,
                    "clickedBefore", clicked);
            slotHandler.clickSlot(slot, mouseButton, type);
            slotHandler.registerSlotAction();
            clicked = true;
            ChatClefDiagnostics.logSlotClick("RETURN", "click_slot_task_after_click", slot, mouseButton, type,
                    "clickedAfter", clicked);
        } else {
            ChatClefDiagnostics.logSlotClick("WAIT", "click_slot_task_timer_blocked", slot, mouseButton, type,
                    "clicked", clicked);
        }
        return null;
    }

    @Override
    protected void onStop(Task interruptTask) {
        ChatClefDiagnostics.logTaskTransition(this, this, interruptTask, "click_slot_task_onStop",
                "slot", ChatClefDiagnostics.slotSummary(slot),
                "mouseButton", mouseButton,
                "slotActionType", type,
                "clicked", clicked);
    }

    @Override
    protected boolean isEqual(Task obj) {
        if (obj instanceof ClickSlotTask task) {
            return task.mouseButton == mouseButton && task.type == type && task.slot.equals(slot);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Clicking " + slot.toString();
    }

    @Override
    public boolean isFinished() {
        ChatClefDiagnostics.logSlotClick("IS_FINISHED", "click_slot_task_isFinished", slot, mouseButton, type,
                "clicked", clicked);
        return clicked;
    }
}
