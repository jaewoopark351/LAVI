package lavi.minecraft.diagnostics.container.store.deposit.event.common;

import java.util.LinkedHashMap;
import java.util.Map;

//20260902_kpopmodder: Centralize ordered field merging without facade cycles.
public final class StoreDepositOrderedFieldSupport {
    private StoreDepositOrderedFieldSupport() {
    }

    public static Object[] merge(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        if ((first.length & 1) != 0 || (second.length & 1) != 0) {
            Object[] concatenated = new Object[first.length + second.length];
            System.arraycopy(first, 0, concatenated, 0, first.length);
            System.arraycopy(second, 0, concatenated, first.length, second.length);
            return concatenated;
        }
        Map<Object, Object> fields = new LinkedHashMap<>();
        putFields(fields, first);
        putFields(fields, second);
        Object[] merged = new Object[fields.size() * 2];
        int index = 0;
        for (Map.Entry<Object, Object> entry : fields.entrySet()) {
            merged[index++] = entry.getKey();
            merged[index++] = entry.getValue();
        }
        return merged;
    }

    private static void putFields(Map<Object, Object> target, Object[] fields) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            target.put(fields[index], fields[index + 1]);
        }
    }
}
