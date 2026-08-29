package lavi.minecraft.diagnostics.container.home.timeout.budget;

//20260828_kpopmodder: Retain only bounded first/last candidate aggregates for cap and terminal evidence.
public final class StoreHomeOperationDiagnosticAggregate {
    private String firstCandidateId = "none";
    private String firstDestinationId = "none";
    private String lastCandidateId = "none";
    private String lastDestinationId = "none";
    private int candidateBoundaryCount;

    public synchronized void observe(Object[] fields, boolean boundary) {
        String candidateId = field(fields, "candidateId");
        String destinationId = field(fields, "destinationId");
        if (!available(candidateId) || !available(destinationId)) {
            return;
        }
        if ("none".equals(firstCandidateId)) {
            firstCandidateId = candidateId;
            firstDestinationId = destinationId;
        }
        lastCandidateId = candidateId;
        lastDestinationId = destinationId;
        if (boundary && candidateBoundaryCount < Integer.MAX_VALUE) {
            candidateBoundaryCount++;
        }
    }

    public synchronized Object[] summaryFields() {
        return new Object[]{
                "firstObservedCandidateId", firstCandidateId,
                "firstObservedDestinationId", firstDestinationId,
                "lastObservedCandidateId", lastCandidateId,
                "lastObservedDestinationId", lastDestinationId,
                "totalCandidateBoundaryCount", candidateBoundaryCount
        };
    }

    private static String field(Object[] fields, String key) {
        if (fields == null) {
            return "unavailable";
        }
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(String.valueOf(fields[index]))) {
                return String.valueOf(fields[index + 1]);
            }
        }
        return "unavailable";
    }

    private static boolean available(String value) {
        return value != null
                && !value.isBlank()
                && !"none".equals(value)
                && !value.startsWith("unavailable");
    }
}
