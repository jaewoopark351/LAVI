package lavi.minecraft.fabric.chatclef.bridge.command.result;

import java.util.HashMap;
import java.util.Map;

//20260805_kpopmodder: Isolate v1 command result map keys from the typed payload object.
final class FabricChatClefCommandResultPayloadMap {
    private static final String REQUEST_ID = "request_id";
    private static final String OK = "ok";
    private static final String STATUS = "status";
    private static final String ERROR_CODE = "error_code";
    private static final String MESSAGE = "message";
    private static final String DATA = "data";

    private FabricChatClefCommandResultPayloadMap() {
    }

    static Map<String, Object> toMap(FabricChatClefCommandResultPayload payload) {
        Map<String, Object> map = new HashMap<>();
        map.put(REQUEST_ID, payload.requestId());
        map.put(OK, payload.status().ok());
        map.put(STATUS, payload.status().wireValue());
        map.put(ERROR_CODE, payload.errorCode());
        map.put(MESSAGE, payload.message());
        map.put(DATA, payload.data());
        return map;
    }
}
