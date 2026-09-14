//#if MC == 12001
//$$ package lavi.minecraft.find.observation.entity;
//$$
//$$ import java.util.Iterator;
//$$ import java.util.function.BooleanSupplier;
//$$ import java.util.function.Supplier;
//$$ import adris.altoclef.trackers.EntityTracker;
//$$ import lavi.minecraft.find.diagnostics.FindEntityScanDiagnostics;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindIdentityDigest;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindCandidateSelection;
//$$ import lavi.minecraft.find.observation.FindObservationPort.Binding;
//$$ import lavi.minecraft.find.observation.FindObservationPort.EntityScan;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.entity.ItemEntity;
//$$ import net.minecraft.entity.mob.MobEntity;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.util.math.BlockPos;
//$$
//$$ //20260914_kpopmodder: Observe one synchronous live membership scope; FIND owns finite visits, matching and ranking.
//$$ public final class MinecraftFindEntityObservation {
//$$     private final MinecraftClient client;
//$$     private final Supplier<EntityTracker> tracker;
//$$     public MinecraftFindEntityObservation(MinecraftClient client, Supplier<EntityTracker> tracker) {
//$$         this.client = client; this.tracker = tracker;
//$$     }
//$$     public EntityScan scan(FindRequest request, Binding binding, int limit, BooleanSupplier withinBudget, FindLog log) {
//$$         return scan(request, binding, limit, withinBudget, log, "INITIAL_OBSERVATION");
//$$     }
//$$     public EntityScan scan(FindRequest request, Binding binding, int limit, BooleanSupplier withinBudget, FindLog log, String phaseRole) {
//$$         requireThread();
//$$         boolean mob = mobRequest(request);
//$$         FindEntityScanDiagnostics diagnostics = mob ? new FindEntityScanDiagnostics() : null;
//$$         Object sourceWorldTime = mob ? sourceWorldTime(binding) : "NOT_EVALUATED";
//$$         if (mob && log != null) log.eventInPhase(phaseRole, "ENTITY_QUERY_STARTED", "reason", "live_membership_query_requested",
//$$                 "source", "TRACKER_LIVE_CLIENT_MEMBERSHIP", "sourceWorldTime", sourceWorldTime,
//$$                 "mode", request.mode(), "distancePolicy", "CLIENT_OBSERVABLE_NO_RADIUS", "originX", binding == null ? "UNAVAILABLE" : binding.x(),
//$$                 "originY", binding == null ? "UNAVAILABLE" : binding.y(), "originZ", binding == null ? "UNAVAILABLE" : binding.z(),
//$$                 "visitLimit", limit, "visited", 0, "matched", 0);
//$$         int visited = 0, matched = 0;
//$$         FindCandidate nearest = null;
//$$         String reason = "complete_loaded_scope_no_match";
//$$         String readStage = "binding_and_initial_deadline", failureStage = "NONE", failureType = "NONE";
//$$         boolean complete = false;
//$$         try {
//$$             if (limit < 0) throw new IllegalArgumentException("negative_entity_visit_limit");
//$$             if (!matches(binding)) reason = "world_or_player_binding_changed";
//$$             else if (!withinBudget.getAsBoolean()) reason = "elapsed_budget_exhausted";
//$$             else {
//$$                 readStage = "source_open";
//$$                 Iterable<Entity> source = mob ? tracker.get().getClientObservedEntities() : ((ClientWorld) binding.world()).getEntities();
//$$                 readStage = "iterator_open";
//$$                 Iterator<Entity> iterator = source.iterator();
//$$                 while (true) {
//$$                     readStage = "deadline_check";
//$$                     if (!withinBudget.getAsBoolean()) { reason = "elapsed_budget_exhausted"; break; }
//$$                     readStage = "membership_end_check";
//$$                     if (!iterator.hasNext()) { complete = true; reason = matched == 0 ? "complete_loaded_scope_no_match" : "complete_loaded_scope_candidate_selected"; break; }
//$$                     if (visited >= limit) { reason = "entity_visit_limit_exhausted"; break; }
//$$                     readStage = "deadline_check";
//$$                     if (!withinBudget.getAsBoolean()) { reason = "elapsed_budget_exhausted"; break; }
//$$                     readStage = "entity_next";
//$$                     Entity entity = iterator.next();
//$$                     visited++;
//$$                     Object sampleId = "NOT_EVALUATED";
//$$                     String rejection = null, rawId = "NOT_EVALUATED";
//$$                     boolean compared = false, rawMatch = false;
//$$                     readStage = "entity_liveness";
//$$                     if (entity == client.player) rejection = "self";
//$$                     else if (!entity.isAlive()) rejection = "dead";
//$$                     else if (entity.isRemoved()) rejection = "removed";
//$$                     else if (mob) {
//$$                         if (!(entity instanceof MobEntity)) {
//$$                             rejection = "wrong_kind";
//$$                             rawId = diagnosticRawId(entity);
//$$                             compared = !rawId.equals("UNAVAILABLE"); rawMatch = compared && rawId.equals(request.target());
//$$                         } else {
//$$                             readStage = "mob_registry_id";
//$$                             rawId = Registries.ENTITY_TYPE.getId(entity.getType()).toString();
//$$                             compared = true; rawMatch = rawId.equals(request.target());
//$$                             if (!rawMatch) rejection = "wrong_target_id";
//$$                         }
//$$                     } else {
//$$                         readStage = "entity_kind_and_target";
//$$                         if (!kindMatches(request, entity)) rejection = "wrong_kind_or_target";
//$$                     }
//$$                     Double distance = null;
//$$                     if (rejection == null) {
//$$                         readStage = "entity_distance";
//$$                         distance = distance(binding, entity);
//$$                         if (!mob && distance > 64.0 * 64.0) rejection = "outside_radius";
//$$                     }
//$$                     if (rejection == null) {
//$$                         matched++;
//$$                         readStage = "candidate_identity_and_position";
//$$                         int localId = entity.getId(); sampleId = localId;
//$$                         nearest = FindCandidateSelection.nearer(nearest, candidate(entity, localId, distance));
//$$                     }
//$$                     else if (diagnostics != null) sampleId = diagnosticLocalId(entity);
//$$                     if (diagnostics != null) diagnostics.capture(sampleId, rawId, compared, rawMatch,
//$$                             rejection == null ? "matched" : rejection, distance);
//$$                 }
//$$                 readStage = "binding_revalidation";
//$$                 if (!matches(binding)) { complete = false; reason = "world_or_player_binding_changed"; }
//$$                 else {
//$$                     readStage = "deadline_check";
//$$                     if (!withinBudget.getAsBoolean()) { complete = false; reason = "elapsed_budget_exhausted"; }
//$$                 }
//$$             }
//$$         } catch (RuntimeException failure) {
//$$             complete = false; reason = "entity_read_failed"; failureStage = readStage; failureType = failure.getClass().getName();
//$$         }
//$$         if (diagnostics != null) diagnostics.emit(log, visited, matched, complete, reason, sourceWorldTime, nearest, failureStage, failureType, phaseRole);
//$$         return new EntityScan(visited, matched, complete, nearest, reason);
//$$     }
//$$     public FindCandidate revalidate(FindRequest request, Binding binding, FindCandidate selected, FindLog log) {
//$$         return revalidate(request, binding, selected, log, "HANDOFF_REVALIDATION");
//$$     }
//$$     public FindCandidate revalidate(FindRequest request, Binding binding, FindCandidate selected, FindLog log, String phaseRole) {
//$$         requireThread();
//$$         FindCandidate current = null;
//$$         String reason = "world_or_player_binding_changed";
//$$         try {
//$$             if (matches(binding)) {
//$$                 Entity entity = ((ClientWorld) binding.world()).getEntityById(selected.entityId());
//$$                 if (entity == null) reason = "selected_entity_absent";
//$$                 else if (entity == client.player) reason = "selected_entity_is_self";
//$$                 else if (!entity.isAlive()) reason = "selected_entity_dead";
//$$                 else if (entity.isRemoved()) reason = "selected_entity_removed";
//$$                 else if (!kindMatches(request, entity)) reason = "selected_kind_or_target_changed";
//$$                 else if (!entity.getUuid().toString().equals(selected.stableSortKey())) reason = "selected_identity_changed";
//$$                 else {
//$$                     double distance = distance(binding, entity);
//$$                     if (!mobRequest(request) && distance > 64.0 * 64.0) reason = "selected_entity_outside_radius";
//$$                     else { current = candidate(entity, entity.getId(), distance); reason = "selected_candidate_revalidated"; }
//$$                 }
//$$             }
//$$         } catch (RuntimeException failure) {
//$$             if (mobRequest(request) && log != null) log.eventInPhase(phaseRole, "ENTITY_REVALIDATION_RESULT", "reason", "selected_entity_read_failed",
//$$                     "localEntityId", selected.entityId(), "revalidated", "UNAVAILABLE");
//$$             throw failure;
//$$         }
//$$         if (mobRequest(request) && log != null) log.eventInPhase(phaseRole, "ENTITY_REVALIDATION_RESULT", "reason", reason,
//$$                 "localEntityId", selected.entityId(), "selectedX", selected.x(), "selectedY", selected.y(), "selectedZ", selected.z(),
//$$                 "currentX", current == null ? "NOT_EVALUATED" : current.x(),
//$$                 "currentY", current == null ? "NOT_EVALUATED" : current.y(),
//$$                 "currentZ", current == null ? "NOT_EVALUATED" : current.z(),
//$$                 "distanceSquared", current == null ? "NOT_EVALUATED" : current.distanceSquared(), "revalidated", current != null);
//$$         return current;
//$$     }
//$$     private boolean kindMatches(FindRequest request, Entity entity) {
//$$         return switch (request.kind()) {
//$$             case "entity" -> entity instanceof MobEntity && Registries.ENTITY_TYPE.getId(entity.getType()).toString().equals(request.target());
//$$             case "player" -> entity instanceof PlayerEntity && entity.getName().getString().equals(request.playerName());
//$$             case "item" -> entity instanceof ItemEntity item && !item.getStack().isEmpty()
//$$                     && Registries.ITEM.getId(item.getStack().getItem()).toString().equals(request.target());
//$$             default -> false;
//$$         };
//$$     }
//$$     private boolean matches(Binding binding) {
//$$         return binding != null && client.world == binding.world() && client.player == binding.player() && client.world != null
//$$                 && binding.dimension().equals(client.world.getRegistryKey().getValue().toString());
//$$     }
//$$     private void requireThread() { if (!client.isOnThread()) throw new IllegalStateException("client_thread_required"); }
//$$     private static boolean mobRequest(FindRequest request) { return request.kind().equals("entity"); }
//$$     private static FindCandidate candidate(Entity entity, int localId, double distance) {
//$$         String uuid = entity.getUuid().toString();
//$$         BlockPos position = entity.getBlockPos();
//$$         return new FindCandidate(localId, FindIdentityDigest.digest(uuid), uuid, position.getX(), position.getY(), position.getZ(), distance);
//$$     }
//$$     private static double distance(Binding binding, Entity entity) {
//$$         double dx = entity.getX() - binding.x(), dy = entity.getY() - binding.y(), dz = entity.getZ() - binding.z();
//$$         return dx * dx + dy * dy + dz * dz;
//$$     }
//$$     private static Object sourceWorldTime(Binding binding) {
//$$         try { return ((ClientWorld) binding.world()).getTime(); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { return "UNAVAILABLE"; }
//$$     }
//$$     private static String diagnosticRawId(Entity entity) {
//$$         try { return Registries.ENTITY_TYPE.getId(entity.getType()).toString(); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { return "UNAVAILABLE"; }
//$$     }
//$$     private static Object diagnosticLocalId(Entity entity) {
//$$         try { return entity.getId(); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { return "UNAVAILABLE"; }
//$$     }
//$$ }
//#endif
