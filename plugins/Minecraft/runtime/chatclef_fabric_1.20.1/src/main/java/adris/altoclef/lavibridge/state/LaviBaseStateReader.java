package adris.altoclef.lavibridge.state;

//20260725_kpopmodder: Added this reader for fields shared by all LAVI state responses.

import adris.altoclef.AltoClef;

import java.util.LinkedHashMap;
import java.util.Map;

public class LaviBaseStateReader {

    private final AltoClef mod;

    public LaviBaseStateReader(AltoClef mod) {
        this.mod = mod;
    }

    public Map<String, Object> baseResponse() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("initialized", mod != null && mod.getTaskRunner() != null);
        result.put("in_game", AltoClef.inGame());
        return result;
    }
}
