//#if MC == 12001
//$$ package lavi.minecraft.find.model;

//$$ //20260914_kpopmodder: Keep the immutable FIND request bound to its admitted catalog generation.
//$$ public record FindRequest(String kind, String target, String mode, String catalogDigest, long resourceGeneration) {
//$$     public FindRequest {
//$$         if (kind == null || target == null || mode == null || catalogDigest == null) {
//$$             throw new IllegalArgumentException("null_find_request_field");
//$$         }
//$$     }

//$$     public String canonicalTargetId() { return kind.equals("player") ? "" : target; }
//$$     public String playerName() { return kind.equals("player") ? target : ""; }
//$$     public String completionMode() { return mode.equals("approach") ? "LOCATE_AND_APPROACH" : "LOCATE_AND_REPORT"; }
//$$     public String observationScope() {
//$$         return switch (kind) {
//$$             case "block" -> "loaded_blocks";
//$$             case "item" -> "loaded_dropped_items";
//$$             case "player" -> "loaded_players";
//$$             default -> "loaded_entities";
//$$         };
//$$     }
//$$     @Override public String toString() { return "FindRequest[kind=" + kind + ",mode=" + mode + "]"; }
//$$ }

//#endif
