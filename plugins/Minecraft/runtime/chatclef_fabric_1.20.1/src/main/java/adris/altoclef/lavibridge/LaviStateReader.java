package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class to expose read-only Minecraft state for LAVI.

import adris.altoclef.AltoClef;
import adris.altoclef.lavibridge.state.LaviBaritoneStateReader;
import adris.altoclef.lavibridge.state.LaviBaseStateReader;
import adris.altoclef.lavibridge.state.LaviControlModeReader;
import adris.altoclef.lavibridge.state.LaviInventoryReader;
import adris.altoclef.lavibridge.state.LaviPlayerStateReader;
import adris.altoclef.lavibridge.state.LaviTaskStateReader;

import java.util.Map;

public class LaviStateReader {

    private final LaviActionRegistry actionRegistry;
    private final LaviBaseStateReader baseStateReader;
    private final LaviControlModeReader controlModeReader;
    private final LaviPlayerStateReader playerStateReader;
    private final LaviTaskStateReader taskStateReader;
    private final LaviBaritoneStateReader baritoneStateReader;
    private final LaviInventoryReader inventoryReader;

    public LaviStateReader(AltoClef mod, LaviActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
        this.baseStateReader = new LaviBaseStateReader(mod);
        this.controlModeReader = new LaviControlModeReader(mod, actionRegistry);
        this.playerStateReader = new LaviPlayerStateReader(mod);
        this.taskStateReader = new LaviTaskStateReader(mod);
        this.baritoneStateReader = new LaviBaritoneStateReader(mod);
        this.inventoryReader = new LaviInventoryReader(mod);
    }

    public Map<String, Object> health() {
        Map<String, Object> result = baseStateReader.baseResponse();
        result.put("service", "lavi-chatclef-bridge");
        result.put("version", "0.1");
        result.put("control_mode", controlModeReader.controlMode());
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = baseStateReader.baseResponse();
        result.put("control_mode", controlModeReader.controlMode());
        result.put("player", playerStateReader.playerStatus());
        result.put("task", taskStateReader.taskStatus());
        result.put("baritone", baritoneStateReader.baritoneStatus());
        result.put("current_action", actionRegistry.currentActionSnapshotOnly());
        return result;
    }

    public Map<String, Object> inventory() {
        Map<String, Object> result = baseStateReader.baseResponse();
        result.put("counts", inventoryReader.inventoryCounts());
        result.put("slots", inventoryReader.inventorySlots());
        return result;
    }
}
