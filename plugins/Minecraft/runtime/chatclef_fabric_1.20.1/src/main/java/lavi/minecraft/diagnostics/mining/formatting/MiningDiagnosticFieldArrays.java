package lavi.minecraft.diagnostics.mining.formatting;

//20260830_kpopmodder: Keep diagnostic field-array composition separate from event emission.
public final class MiningDiagnosticFieldArrays {
    private MiningDiagnosticFieldArrays() {
    }

    public static Object[] merge(Object[]... arrays) {
        int length = 0;
        for (Object[] array : arrays) {
            if (array != null) {
                length += array.length;
            }
        }
        Object[] merged = new Object[length];
        int offset = 0;
        for (Object[] array : arrays) {
            if (array == null || array.length == 0) {
                continue;
            }
            System.arraycopy(array, 0, merged, offset, array.length);
            offset += array.length;
        }
        return merged;
    }
}
