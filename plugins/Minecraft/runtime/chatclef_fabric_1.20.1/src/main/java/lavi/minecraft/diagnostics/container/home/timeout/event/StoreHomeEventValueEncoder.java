package lavi.minecraft.diagnostics.container.home.timeout.event;

//20260828_kpopmodder: Keep STORE_HOME key=value payload tokens parseable without changing values used by Tasks.
final class StoreHomeEventValueEncoder {
    private StoreHomeEventValueEncoder() {
    }

    static Object[] encode(Object[] fields) {
        if (fields == null || fields.length == 0) {
            return new Object[0];
        }
        Object[] encoded = fields.clone();
        for (int index = 0; index < encoded.length; index += 2) {
            encoded[index] = token(encoded[index]);
            if (index + 1 < encoded.length) {
                encoded[index + 1] = token(encoded[index + 1]);
            }
        }
        return encoded;
    }

    private static String token(Object value) {
        String text;
        try {
            text = String.valueOf(value);
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
        return text
                .replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")
                .replace("\t", "%09")
                .replace(" ", "%20");
    }
}
