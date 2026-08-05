package lavi.minecraft.diagnostics.command;

//20260805_kpopmodder: Keep active command diagnostic fields typed while preserving log-only ownership.
public final class DiagnosticCommandContextSnapshot {
    private static final DiagnosticCommandContextSnapshot UNAVAILABLE = new DiagnosticCommandContextSnapshot(
            false,
            "",
            "",
            "",
            "unavailable",
            "",
            "",
            "no_provider"
    );

    private final boolean available;
    private final String requestId;
    private final String correlationId;
    private final String sessionId;
    private final String connectionGeneration;
    private final String command;
    private final String source;
    private final String error;

    private DiagnosticCommandContextSnapshot(boolean available,
                                             String requestId,
                                             String correlationId,
                                             String sessionId,
                                             String connectionGeneration,
                                             String command,
                                             String source,
                                             String error) {
        this.available = available;
        this.requestId = nullToEmpty(requestId);
        this.correlationId = nullToEmpty(correlationId);
        this.sessionId = nullToEmpty(sessionId);
        this.connectionGeneration = nullToEmpty(connectionGeneration);
        this.command = nullToEmpty(command);
        this.source = nullToEmpty(source);
        this.error = nullToEmpty(error);
    }

    public static DiagnosticCommandContextSnapshot unavailable() {
        return UNAVAILABLE;
    }

    public static DiagnosticCommandContextSnapshot unavailable(String error) {
        return new DiagnosticCommandContextSnapshot(false, "", "", "", "unavailable", "", "", error);
    }

    public static DiagnosticCommandContextSnapshot active(String requestId,
                                                          String correlationId,
                                                          String sessionId,
                                                          long connectionGeneration,
                                                          String command,
                                                          String source) {
        return new DiagnosticCommandContextSnapshot(
                true,
                requestId,
                correlationId,
                sessionId,
                Long.toString(connectionGeneration),
                command,
                source,
                "none"
        );
    }

    public Object[] fields() {
        return new Object[]{
                "commandContextAvailable", available,
                "commandRequestId", requestId,
                "commandCorrelationId", correlationId,
                "commandSessionId", sessionId,
                "commandConnectionGeneration", connectionGeneration,
                "commandText", command,
                "commandSource", source,
                "commandContextError", error
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
