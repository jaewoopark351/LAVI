//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.Debug;
//$$ import adris.altoclef.eventbus.EventBus;
//$$ import adris.altoclef.eventbus.Subscription;
//$$ import adris.altoclef.eventbus.events.TaskFinishedEvent;
//$$ import adris.altoclef.tasksystem.Task;
//$$ import lavi.minecraft.find.catalog.FindResourceBinding;
//$$ import lavi.minecraft.find.result.FindTaskResultSource;
//$$ import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.text.Text;

//$$ //20260914_kpopmodder: One command-owned native subscription presents only its exact natural-completion lifetime.
//$$ public final class FindNativeCompletionObserver {
//$$     private Task expectedTask;
//$$     private FindTaskResultSource source;
//$$     private UserRootLifetime lifetime;
//$$     private Object world, player;
//$$     private Subscription<TaskFinishedEvent> subscription;

//$$     public void prepare(Task task, FindTaskResultSource source, AltoClef mod) {
//$$         retire("replaced_native_observation");
//$$         expectedTask = task; this.source = source; world = mod.getWorld(); player = mod.getPlayer();
//$$         subscription = EventBus.subscribe(TaskFinishedEvent.class, this::completed);
//$$         log("SUBSCRIBED", "pending_matching_root");
//$$     }
//$$     public void bind(UserRootLifetime lifetime) {
//$$         if (source == null || lifetime == null || !lifetime.owns(expectedTask) || lifetime.completion() != null) {
//$$             log("BOUND", "invalid_root_lifetime"); retire("invalid_root_lifetime"); return;
//$$         }
//$$         if (this.lifetime != null && this.lifetime != lifetime) {
//$$             log("BOUND", "root_lifetime_rebinding_rejected"); retire("root_lifetime_rebinding_rejected"); return;
//$$         }
//$$         this.lifetime = lifetime;
//$$         source.bindUserRootLifetime(lifetime);
//$$         log("BOUND", "matching_root_lifetime");
//$$     }
//$$     public void onEndClientTick() {
//$$         if (source == null) return;
//$$         var client = MinecraftClient.getInstance();
//$$         var mod = AltoClef.getInstance();
//$$         if (client.world != world || client.player != player) { retire("native_world_or_player_changed"); return; }
//$$         if (mod == null || mod.getUserTaskChain() == null || mod.getUserTaskChain().getCurrentTask() != expectedTask
//$$                 || lifetime == null || lifetime.completion() != null || mod.getUserTaskChain().currentRootLifetime() != lifetime) {
//$$             retire("native_root_no_longer_owned");
//$$             return;
//$$         }
//$$         source.observeClientTick();
//$$     }
//$$     private void completed(TaskFinishedEvent event) {
//$$         if (source == null || event.lastTaskRan != expectedTask || event.rootLifetime() != lifetime || lifetime == null || !lifetime.owns(expectedTask)) return;
//$$         var completion = lifetime.completion();
//$$         var client = MinecraftClient.getInstance();
//$$         boolean natural = completion != null && completion.stopStateAvailable() && !completion.stopped();
//$$         boolean bound = client.world == world && client.player == player && client.world != null && client.player != null;
//$$         var outcome = source.outcome();
//$$         boolean resourceMatched = outcome != null && FindResourceBinding.currentMatches(outcome.request());
//$$         boolean present = natural && bound && outcome != null && !source.nativePresentationSuppressed()
//$$                 && (outcome.dimension().equals(client.world.getRegistryKey().getValue().toString())
//$$                         || !outcome.satisfied() && outcome.dimension().isEmpty());
//$$         if (present) {
//$$             String response = resourceMatched ? FindNativeResultRenderer.render(outcome, expectedTask)
//$$                     : "찾기 요청의 대상 정보가 바뀌어서 결과를 확인할 수 없어.";
//$$             if (!response.isEmpty()) client.player.sendMessage(Text.literal(response), false);
//$$         }
//$$         log("COMPLETED", present ? resourceMatched ? "native_private_message_returned" : "catalog_resource_binding_changed"
//$$                 : "suppressed_or_unverified_completion");
//$$         retire("native_matching_completion");
//$$     }
//$$     public void retire(String reason) {
//$$         try {
//$$             if (source != null) source.retireOwnedResources(reason);
//$$         } finally {
//$$         if (subscription != null) EventBus.unsubscribe(subscription);
//$$         if (source != null && !source.nativePresentationSuppressed()) {
//$$             try { source.diagnosticRetired(reason); }
//$$             catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$         }
//$$         expectedTask = null; source = null; lifetime = null; world = null; player = null; subscription = null;
//$$         }
//$$     }
//$$     private void log(String event, String reason) {
//$$         try { Debug.logInternal("LAVI FIND native event=" + event + " operation=" + (source == null ? "UNBOUND" : source.operationId()) + " reason=" + reason); }
//$$         catch (RuntimeException | LinkageError | AssertionError ignored) { }
//$$     }
//$$ }

//#endif
