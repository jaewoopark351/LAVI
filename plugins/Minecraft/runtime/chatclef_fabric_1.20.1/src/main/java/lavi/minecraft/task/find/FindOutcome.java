//#if MC == 12001
//20260914_kpopmodder: Freeze FIND evidence on the owning task; cancelled/interrupted work is never success.
package lavi.minecraft.task.find;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FindOutcome(String operationId, FindRequest request, String code,
                          String kind, String registryId, String label, String dimension,
                          List<Integer> position, int radius, int scanned, boolean scanComplete,
                          boolean observed, String entityUuid, int languageWarnings,
                          List<String> suggestions) {
    public FindOutcome {
        position = List.copyOf(position);
        suggestions = List.copyOf(suggestions);
    }
    public boolean success() { return code.equals("FOUND") || code.equals("ARRIVED"); }

    public Map<String, Object> toMap() {
        var data = new LinkedHashMap<String, Object>();
        data.put("schema_version", 1);
        data.put("operation_id", operationId);
        data.put("query", request.query());
        data.put("requested_kind", request.kind());
        data.put("mode", request.mode());
        data.put("code", code);
        data.put("kind", kind);
        data.put("registry_id", registryId);
        data.put("label", label);
        data.put("dimension", dimension);
        data.put("position", position);
        data.put("radius", radius);
        data.put("scanned", scanned);
        data.put("scan_complete", scanComplete);
        data.put("observed", observed);
        data.put("arrived", code.equals("ARRIVED"));
        data.put("entity_uuid", entityUuid);
        data.put("language_warnings", languageWarnings);
        data.put("suggestions", suggestions);
        data.put("scope", kind.equals("block") ? "loaded_block_cube" : "loaded_entity_sphere");
        return data;
    }

    public String koreanMessage() {
        String target = label.isBlank() ? request.query() : label;
        String location = position.size() == 3 ? String.format("%d, %d, %d", position.get(0), position.get(1), position.get(2)) : "";
        return switch (code) {
            case "FOUND" -> target + " 위치를 확인했어. 좌표 " + location + ". 이동하지 않았어.";
            case "ARRIVED" -> target + " 근처에 도착했어. 대상 좌표 " + location + ".";
            case "NOT_FOUND" -> "현재 로드된 탐색 범위 " + radius + "블록 안에서 " + target + "을 찾지 못했어.";
            case "UNKNOWN_TARGET" -> "대상 이름을 해석하지 못했어. 종류와 namespace:id를 지정해 줘.";
            case "AMBIGUOUS_TARGET" -> "같은 이름의 대상이 여러 종류야. 종류와 ID를 지정해 줘: " + String.join(", ", suggestions);
            case "TARGET_LOST" -> "찾았던 대상이 사라지거나 탐색 범위를 벗어났어. 도착으로 처리하지 않았어.";
            case "NO_APPROACH" -> "대상은 찾았지만 가까이 설 수 있는 안전한 위치를 찾지 못했어.";
            case "APPROACH_TIMEOUT" -> "대상은 찾았지만 제한 시간 안에 접근하지 못했어.";
            case "SEARCH_LIMIT" -> "탐색 제한에 도달했어. 확인하지 못한 범위까지 없다고 판단하지 않았어.";
            case "SCOPE_CHANGED" -> "월드나 차원이 바뀌어서 찾기를 종료했어.";
            case "PLAYER_UNAVAILABLE" -> "플레이어 상태를 확인할 수 없어서 찾기를 종료했어.";
            default -> "찾기를 계속할 수 없어서 종료했어. 로그를 확인해 줘.";
        };
    }
}
//#endif
