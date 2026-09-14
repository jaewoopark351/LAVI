//#if MC == 12001
//$$ package lavi.minecraft.fabric.chatclef.bridge.transport.find;

//$$ import lavi.minecraft.find.catalog.FindCatalogSnapshot;
//$$ import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
//$$ import lavi.minecraft.fabric.chatclef.bridge.transport.session.FabricChatClefAcceptedSessionIdentity;
//$$ import java.util.Map;
//$$ import java.util.concurrent.CompletableFuture;
//$$ import java.util.function.Supplier;
//$$ import java.util.function.LongSupplier;

//$$ //20260914_kpopmodder: One finite page exchange per resource/socket generation with at most one pending send.
//$$ public final class FabricChatClefFindCatalogPublisher {
//$$     private final FabricChatClefFindCatalogPageSender sender;
//$$     private final Supplier<FindCatalogSnapshot> catalogs;
//$$     private final FabricChatClefBridgeDiagnostics diagnostics;
//$$     private final LongSupplier clock;
//$$     private FindCatalogSnapshot attempted;
//$$     private FabricChatClefAcceptedSessionIdentity identity;
//$$     private CompletableFuture<?> pending;
//$$     private int nextPage;
//$$     private long startedNanos;
//$$     private long latestResourceGeneration;
//$$     private boolean invalidated, done;
//$$     public FabricChatClefFindCatalogPublisher(FabricChatClefFindCatalogPageSender sender,
//$$             Supplier<FindCatalogSnapshot> catalogs, FabricChatClefBridgeDiagnostics diagnostics) {
//$$         this(sender, catalogs, diagnostics, System::nanoTime);
//$$     }
//$$     public FabricChatClefFindCatalogPublisher(FabricChatClefFindCatalogPageSender sender,
//$$             Supplier<FindCatalogSnapshot> catalogs, FabricChatClefBridgeDiagnostics diagnostics, LongSupplier clock) {
//$$         this.sender = sender; this.catalogs = catalogs; this.diagnostics = diagnostics;
//$$         this.clock = clock;
//$$     }
//$$     public void tick() {
//$$         var active = sender.identity();
//$$         var snapshot = catalogs.get();
//$$         if (snapshot != null) latestResourceGeneration = Math.max(latestResourceGeneration, snapshot.resourceGeneration());
//$$         if (active == null) { identity = null; attempted = null; pending = null; return; }
//$$         boolean socketReplaced = identity == null || active.javaSocketGeneration() != identity.javaSocketGeneration()
//$$                 || active.serverConnectionGeneration() != identity.serverConnectionGeneration()
//$$                 || !active.sessionId().equals(identity.sessionId());
//$$         if (socketReplaced || attempted != snapshot) {
//$$             // Wait for the sole owned send to settle before a replacement on the same socket.
//$$             if (!socketReplaced && pending != null && !pending.isDone()) {
//$$                 if (!done && clock.getAsLong() - startedNanos >= 10_000_000_000L) {
//$$                     done = true; log("FIND_CATALOG_EXCHANGE_FAILED", "replacement_wait_deadline");
//$$                 }
//$$                 return;
//$$             }
//$$             identity = active; attempted = snapshot; pending = null;
//$$             nextPage = 0; startedNanos = clock.getAsLong(); invalidated = false; done = false;
//$$             log("FIND_CATALOG_EXCHANGE_STARTED", "catalog_or_session_replaced");
//$$         }
//$$         if (done) return;
//$$         if (clock.getAsLong() - startedNanos >= 10_000_000_000L) {
//$$             done = true; log("FIND_CATALOG_EXCHANGE_FAILED", "exchange_deadline"); return;
//$$         }
//$$         if (pending != null) {
//$$             if (!pending.isDone()) return;
//$$             if (pending.isCompletedExceptionally() || pending.isCancelled()) {
//$$                 pending = null; done = true; log("FIND_CATALOG_EXCHANGE_FAILED", "send_future_failed"); return;
//$$             }
//$$             pending = null;
//$$             log("FIND_CATALOG_SEND_COMPLETED", "transport_future_completed_not_receiver_ack");
//$$         }
//$$         if (!invalidated) {
//$$             long generation = snapshot == null ? latestResourceGeneration : snapshot.resourceGeneration();
//$$             String reason = snapshot != null && snapshot.complete() ? "CATALOG_REPLACED" : "CATALOG_INCOMPLETE";
//$$             pending = sender.send(identity, "find_catalog_invalidated", Map.of(
//$$                     "catalog_version", 1, "resource_generation", generation, "reason", reason));
//$$             invalidated = true;
//$$             log("FIND_CATALOG_SEND_SUBMITTED", "invalidation_submitted");
//$$             return;
//$$         }
//$$         if (snapshot == null || !snapshot.complete()) { done = true; log("FIND_CATALOG_EXCHANGE_FAILED", "source_incomplete"); return; }
//$$         if (nextPage == snapshot.pages().size()) { done = true; log("FIND_CATALOG_EXCHANGE_COMPLETED", "all_send_futures_completed_not_receiver_ack"); return; }
//$$         try { pending = sender.send(identity, "find_catalog_page", snapshot.pages().get(nextPage)); }
//$$         catch (RuntimeException transportFailure) { done = true; log("FIND_CATALOG_EXCHANGE_FAILED", "transport_submission_exception"); return; }
//$$         nextPage++;
//$$         log("FIND_CATALOG_SEND_SUBMITTED", "page_submitted");
//$$     }
//$$     private void log(String event, String reason) {
//$$         try { diagnostics.info("event=" + event + " reason=" + reason
//$$                 + " socketGeneration=" + (identity == null ? -1 : identity.javaSocketGeneration())
//$$                 + " resourceGeneration=" + (attempted == null ? latestResourceGeneration : attempted.resourceGeneration())
//$$                 + " page=" + nextPage + " pages=" + (attempted == null ? 0 : attempted.pages().size())); }
//$$         catch (RuntimeException | LinkageError ignored) { }
//$$     }
//$$ }
//#endif
