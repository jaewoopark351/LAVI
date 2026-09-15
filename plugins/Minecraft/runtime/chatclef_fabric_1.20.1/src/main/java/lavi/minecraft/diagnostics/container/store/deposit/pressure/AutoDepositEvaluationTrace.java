package lavi.minecraft.diagnostics.container.store.deposit.pressure;

import java.util.LinkedHashMap;
import java.util.Map;

//20260913_kpopmodder: Retain only values reached by one real automatic-pressure evaluation.
public final class AutoDepositEvaluationTrace {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public AutoDepositEvaluationTrace(long sequence, long tick, String state, boolean pending) {
        fields.put("evaluationSequence", sequence);
        fields.put("captureTick", tick);
        fields.put("stateBefore", state);
        fields.put("thresholdPendingBefore", pending);
        //20260915_openai: Unreached native/legacy admission inputs remain explicitly NOT_EVALUATED.
        for (String key : new String[]{"inGame", "bridgeEnabled", "runnerActive", "automationAvailable",
                "explicitStopBefore", "executionPermitted", "pressureRead", "occupiedSlots",
                "totalSlots", "thresholdReached", "lowWaterReached", "trustedRevisionChanged",
                "fingerprintChanged", "activeUserTask", "cachedUserChainSelected",
                "existingDepositTask", "workingSetStatus", "planningStatus"}) {
            fields.put(key, "NOT_EVALUATED");
        }
        fields.put("inventoryScope", "PlayerInventory.main");
        fields.put("thresholdRatio", "9/10");
        fields.put("lowWaterRatio", "7/9");
    }

    public void record(String key, Object value) {
        if (fields.containsKey(key) || fields.size() < 48) {
            fields.put(key, value);
        }
    }

    public Object[] fields() {
        Object[] result = new Object[fields.size() * 2];
        int index = 0;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            result[index++] = entry.getKey();
            result[index++] = entry.getValue();
        }
        return result;
    }
}
