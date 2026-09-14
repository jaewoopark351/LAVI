//#if MC == 12001
//$$ package lavi.minecraft.fabric.chatclef.bridge.command.result.find;

//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.result.FindOutcome;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;
//$$ import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
//$$ import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
//$$ import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
//$$ import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
//$$ import java.nio.charset.StandardCharsets;
//$$ import java.security.MessageDigest;
//$$ import java.security.NoSuchAlgorithmException;
//$$ import java.util.HexFormat;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.Set;
//$$ import java.util.function.Predicate;

//$$ //20260914_kpopmodder: Project only an immutable FIND outcome from the exact naturally completed command root.
//$$ public final class FabricChatClefFindResultProjector {
//$$     private final Predicate<FindRequest> resourceBinding;
//$$     public FabricChatClefFindResultProjector() {
//$$         this(lavi.minecraft.find.catalog.FindResourceBinding::currentMatches);
//$$     }
//$$     public FabricChatClefFindResultProjector(Predicate<FindRequest> resourceBinding) {
//$$         this.resourceBinding = java.util.Objects.requireNonNull(resourceBinding);
//$$     }
//$$     private static final Set<String> COMPLETED_RESULTS = Set.of(
//$$             "FOUND_AND_REPORTED", "FOUND_AND_IN_SAFE_RANGE", "ALREADY_IN_SAFE_RANGE",
//$$             "NOT_OBSERVED_IN_LOADED_SCOPE");

//$$     public FabricChatClefCommandResultPayload fromMatchingCompletion(
//$$             String requestId, FabricChatClefCommandResultDataPayload data,
//$$             Task boundRoot, FabricChatClefCommandTerminationObservation observation,
//$$             boolean userStopBound) {
//$$         if (!(boundRoot instanceof FindTaskResultSource source)) return null;
//$$         if (userStopBound || observation == null || observation.task() != boundRoot) return null;
//$$         if (!(observation.rootLifetime() instanceof lavi.minecraft.integration.lifecycle.root.UserRootLifetime lifetime)
//$$                 || !lifetime.owns(boundRoot) || lifetime.completion() == null) return null;
//$$         var completion = lifetime.completion();
//$$         // Existing chain cleanup can set Task.stopped after natural completion: use its frozen authoritative boundary.
//$$         if (!completion.stopStateAvailable() || completion.stopped()) return null;
//$$         FindOutcome outcome = source.outcome();
//$$         if (outcome == null || outcome.request() != source.request()
//$$                 || !outcome.operationId().equals(source.operationId())) {
//$$             return FabricChatClefCommandResult.unknown(requestId,
//$$                     "FIND Task completed without a matching immutable outcome.", data);
//$$         }
//$$         if (!resourceBinding.test(outcome.request())) {
//$$             try { lavi.minecraft.diagnostics.ChatClefDiagnostics.logLifecycleBoundary(
//$$                     "FIND_RESULT_PUBLICATION_REJECTED", "resource_binding_changed", boundRoot,
//$$                     "requestId", requestId, "operationId", outcome.operationId(), "coordinatesPublished", false); }
//$$             catch (RuntimeException | LinkageError ignored) { }
//$$             FabricChatClefFindDiagnosticRetirement.observe(boundRoot, "resource_binding_changed_before_publication");
//$$             return FabricChatClefCommandResult.unknown(requestId,
//$$                     "FIND catalog changed before result publication; request binding is unverified.", data);
//$$         }
//$$         var payload = new LinkedHashMap<String, Object>();
//$$         FindRequest request = outcome.request();
//$$         boolean approach = request.mode().equals("approach");
//$$         payload.put("completion_mode", request.completionMode());
//$$         payload.put("target_kind", request.kind());
//$$         payload.put("observation_scope", request.observationScope());
//$$         payload.put("find_result", outcome.findResult());
//$$         payload.put("find_satisfied", outcome.satisfied());
//$$         payload.put("reason", outcome.reason());
//$$         if (request.kind().equals("player")) payload.put("player_identity_digest", playerRequestDigest(request.playerName()));
//$$         else {
//$$             payload.put("canonical_target_id", request.canonicalTargetId());
//$$             payload.put("catalog_digest", request.catalogDigest());
//$$             payload.put("resource_generation", request.resourceGeneration());
//$$         }
//$$         if (outcome.satisfied()) {
//$$             var candidate = outcome.candidate();
//$$             payload.put("candidate_identity_digest", candidate.identityDigest());
//$$             payload.put("dimension", outcome.dimension());
//$$             payload.put("x", candidate.x());
//$$             payload.put("y", candidate.y());
//$$             payload.put("z", candidate.z());
//$$             if (approach) payload.put("safe_distance_satisfied", true);
//$$         }
//$$         var values = new LinkedHashMap<String, Object>(data.toMap());
//$$         values.put("find_operation_id", outcome.operationId());
//$$         values.put("effect_profile_id", approach ? "fabric_chatclef_find_approach" : "fabric_chatclef_find_observation");
//$$         values.put("effect_profile_version", 1);
//$$         values.put("effect_kind", approach ? "find_approach" : "find_observation");
//$$         values.put("effect_payload", payload);
//$$         var projected = FabricChatClefCommandResultDataPayload.fromMap(values);
//$$         try { lavi.minecraft.diagnostics.ChatClefDiagnostics.logLifecycleBoundary(
//$$                 "FIND_RESULT_PROJECTED", "matching_task_finished", boundRoot,
//$$                 "requestId", requestId, "operationId", outcome.operationId(),
//$$                 "targetKind", request.kind(), "completionMode", request.completionMode(),
//$$                 "findResult", outcome.findResult(), "findSatisfied", outcome.satisfied()); }
//$$         catch (RuntimeException | LinkageError ignored) { }
//$$         FabricChatClefFindDiagnosticRetirement.observe(boundRoot, "matching_task_finished");
//$$         return COMPLETED_RESULTS.contains(outcome.findResult())
//$$                 ? FabricChatClefCommandResult.completed(requestId, "FIND query reached a verified terminal outcome.", projected)
//$$                 : FabricChatClefCommandResult.failed(requestId, "FIND query ended without satisfying the request.", projected);
//$$     }

//$$     public static String playerRequestDigest(String name) {
//$$         try {
//$$             return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
//$$                     .digest(name.getBytes(StandardCharsets.UTF_8)));
//$$         } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
//$$     }
//$$ }
//#endif
