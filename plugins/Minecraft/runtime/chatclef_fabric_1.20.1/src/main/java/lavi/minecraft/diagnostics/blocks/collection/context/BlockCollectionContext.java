package lavi.minecraft.diagnostics.blocks.collection.context;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.observation.format.ObservationFields;

//20260913_kpopmodder: Freeze diagnostic command metadata without querying gameplay state or collections.
public record BlockCollectionContext(long tick, long nanos, long threadId, String threadName,
        String requestId, String correlationId, String sessionId, String connectionGeneration,
        String commandContextAvailable, String commandContextError) {
    public static BlockCollectionContext capture() {
        Object[] context = ChatClefDiagnostics.currentCommandContextFields();
        Thread thread = Thread.currentThread();
        return new BlockCollectionContext(ChatClefDiagnostics.currentClientTickId(), System.nanoTime(),
                thread.getId(), ObservationFields.text(thread.getName()),
                field(context, "commandRequestId"), field(context, "commandCorrelationId"),
                field(context, "commandSessionId"), field(context, "commandConnectionGeneration"),
                field(context, "commandContextAvailable"), field(context, "commandContextError"));
    }

    private static String field(Object[] fields, String key) {
        if (fields != null) for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) return ObservationFields.text(fields[index + 1]);
        }
        return "UNAVAILABLE";
    }

    public Object[] fields() {
        return new Object[]{"entryTick", tick, "entryNanos", nanos, "entryThreadId", threadId,
                "entryThreadName", threadName,
                "contextAuthority", "ACTIVE_COMMAND_AT_COLLECTION_ENTRY_NOT_PATH_SUBMISSION_BINDING",
                "entryCommandContextAvailable", commandContextAvailable, "entryCommandContextError", commandContextError,
                "entryActiveCommandRequestId", requestId, "entryActiveCommandCorrelationId", correlationId,
                "entryActiveCommandSessionId", sessionId, "entryActiveCommandConnectionGeneration", connectionGeneration};
    }
}
