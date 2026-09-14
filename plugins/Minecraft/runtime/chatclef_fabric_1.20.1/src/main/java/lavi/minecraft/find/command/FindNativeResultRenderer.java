//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import lavi.minecraft.find.catalog.FindCatalogRuntime;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.result.FindOutcome;

//$$ //20260914_kpopmodder: Native output uses deterministic Korean text from verified FIND outcomes only.
//$$ public final class FindNativeResultRenderer {
//$$     private FindNativeResultRenderer() { }
//$$     public static String render(FindOutcome outcome) {
//$$         return render(outcome, null); // Legacy callers cannot self-certify a new parent snapshot's root.
//$$     }
//$$     public static String render(FindOutcome outcome, Object root) {
//$$         if (!lavi.minecraft.find.result.FindTerminalReasonContract.validates(outcome, root)) return "찾기 결과를 확인할 수 없어.";
//$$         String label = label(outcome.request());
//$$         return switch (outcome.findResult()) {
//$$             case "FOUND_AND_REPORTED" -> label + "을 찾았어. 확인한 위치는 " + coordinates(outcome) + "이야.";
//$$             case "FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE" -> label + "을 찾아서 가까이 도착했어. 확인한 위치는 " + coordinates(outcome) + "이야.";
//$$             case "NOT_OBSERVED_IN_LOADED_SCOPE" -> "불러온 범위에서는 " + label + "을 확인하지 못했어.";
//$$             case "OBSERVATION_BOUNDS_EXHAUSTED" -> "탐색 한도에 도달해서 전부 확인하지는 못했어.";
//$$             case "TARGET_LOST", "CANDIDATE_NOT_REVALIDATABLE" -> "확인하던 대상을 지금은 확인할 수 없어.";
//$$             case "UNREACHABLE" -> outcome.reason().equals("post_discovery_approach_unreachable")
//$$                     ? label + "은 발견했지만 허용된 이동 방식으로 가까이 갈 수는 없었어."
//$$                     : outcome.reason().equals("exploration_route_unavailable") || outcome.reason().equals("exploration_no_progress")
//$$                     ? "탐험을 계속할 수 없어서 대상을 찾지는 못했어." : "허용된 이동 방식으로 가까이 갈 수는 없었어.";
//$$             case "TIMEOUT" -> outcome.reason().equals("exploration_step_deadline_exhausted") ? "탐험 이동의 시간 한도에 도달해서 탐색을 계속하지는 못했어."
//$$                     : outcome.reason().equals("native_approach_step_deadline_exhausted") ? "접근 이동의 시간 한도에 도달해서 가까이 도착하지는 못했어."
//$$                     : outcome.reason().equals("discovery_deadline_exhausted")
//$$                     ? "찾기 시간 한도에 도달해서 탐색을 완료하지는 못했어."
//$$                     : outcome.reason().equals("approach_deadline_exhausted") ? "접근 시간 한도에 도달해서 가까이 도착하지는 못했어."
//$$                     : "찾기 작업의 시간 한도에 도달해서 요청을 완료하지는 못했어.";
//$$             case "STOPPED" -> "";
//$$             case "INTERRUPTED" -> "찾기 작업이 중단됐어.";
//$$             default -> "찾기 결과를 확인할 수 없어.";
//$$         };
//$$     }
//$$     private static String coordinates(FindOutcome outcome) {
//$$         if (!outcome.satisfied() || outcome.candidate() == null) return "";
//$$         var candidate = outcome.candidate();
//$$         return "X " + candidate.x() + ", Y " + candidate.y() + ", Z " + candidate.z();
//$$     }
//$$     private static String label(FindRequest request) {
//$$         if (request.kind().equals("player")) return request.playerName();
//$$         var catalog = FindCatalogRuntime.instance().currentSnapshot();
//$$         if (catalog != null && catalog.complete() && catalog.digest().equals(request.catalogDigest())) {
//$$             var record = catalog.record(request.kind(), request.canonicalTargetId());
//$$             if (record != null && !record.koreanName().isEmpty()) return record.koreanName();
//$$         }
//$$         return request.canonicalTargetId();
//$$     }
//$$ }

//#endif
