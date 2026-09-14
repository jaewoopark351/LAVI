//#if MC == 12001
//$$ package lavi.minecraft.fabric.chatclef.bridge.command.admission.find;

//$$ import adris.altoclef.commandsystem.CommandException;
//$$ import lavi.minecraft.find.catalog.FindCatalogSnapshot;
//$$ import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
//$$ import lavi.minecraft.diagnostics.ChatClefDiagnostics;
//$$ import java.util.Map;

//$$ //20260914_kpopmodder: Reject stale interpreted FIND bindings before an engine invocation or task can begin.
//$$ public final class FabricChatClefFindAdmissionBindingGuard {
//$$     private FabricChatClefFindAdmissionBindingGuard() {}
//$$     public static void validate(String command, String commandPrefix, FabricChatClefCommandContext context,
//$$             FindCatalogSnapshot catalog) throws CommandException {
//$$         String bare = commandPrefix != null && !commandPrefix.isEmpty() && command.startsWith(commandPrefix)
//$$                 ? command.substring(commandPrefix.length()) : command;
//$$         if (!bare.startsWith("find ")) return;
//$$         Object natural = context.request().metadata == null ? null : context.request().metadata.get("natural_language");
//$$         if (!(natural instanceof Map<?,?> language) || !(language.get("translation") instanceof Map<?,?> translation)) return;
//$$         if (!(translation.get("intent") instanceof Map<?,?> intent) || !"find".equals(intent.get("intent_type"))) return;
//$$         String[] units = bare.split("\\s+");
//$$         if (units.length >= 2 && units[1].equals("player")) return;
//$$         Map<?,?> binding = translation.get("data") instanceof Map<?,?> values ? values : Map.of();
//$$         boolean available = catalog != null && catalog.complete();
//$$         boolean digestMatched = available && catalog.digest().equals(binding.get("find_catalog_digest"));
//$$         boolean resourceMatched = available && integerEquals(binding.get("find_resource_generation"), catalog.resourceGeneration());
//$$         boolean sessionMatched = context.sessionId().equals(binding.get("find_session_id"));
//$$         boolean connectionMatched = integerEquals(binding.get("find_connection_generation"), context.serverConnectionGeneration());
//$$         boolean matched = available && digestMatched && resourceMatched && sessionMatched && connectionMatched;
//$$         try { ChatClefDiagnostics.logLifecycleBoundary("FIND_ADMISSION_BINDING", matched ? "binding_matched" : "binding_stale_or_unavailable", null,
//$$                 "requestId", context.requestId(), "correlationId", context.correlationId(),
//$$                 "connectionGeneration", context.serverConnectionGeneration(),
//$$                 "catalogAvailable", available, "digestMatched", digestMatched, "resourceMatched", resourceMatched,
//$$                 "sessionMatched", sessionMatched, "connectionMatched", connectionMatched,
//$$                 "actualResourceGeneration", catalog == null ? "UNAVAILABLE" : catalog.resourceGeneration(),
//$$                 "bindingMatched", matched, "taskStarted", false); }
//$$         catch (RuntimeException | LinkageError ignored) { }
//$$         if (!matched) throw new CommandException("FIND INVALID_TARGET: stale_or_unavailable_catalog_binding");
//$$     }
//$$     private static boolean integerEquals(Object value, long expected) {
//$$         return (value instanceof Integer || value instanceof Long) && ((Number)value).longValue() == expected;
//$$     }
//$$ }
//#endif
