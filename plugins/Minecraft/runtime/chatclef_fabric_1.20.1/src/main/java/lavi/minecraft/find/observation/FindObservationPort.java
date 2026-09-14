//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import java.util.function.BooleanSupplier;

//$$ //20260914_kpopmodder: Client reads are separated from finite operation state for ownership and testing.
//$$ public interface FindObservationPort {
//$$     record Binding(Object world, Object player, String dimension, double x, double y, double z, int bottomY, int topY) { }
//$$     record EntityScan(int visited, int matched, boolean complete, FindCandidate nearest, String reason) {
//$$         public EntityScan(int visited, int matched, boolean complete, FindCandidate nearest) {
//$$             this(visited, matched, complete, nearest, complete ? "complete_loaded_scope" : "entity_visit_limit_exhausted");
//$$         }
//$$     }
//$$     Binding binding();
//$$     boolean matches(Binding original);
//$$     default boolean resourceBindingMatches(FindRequest request) { return true; }
//$$     EntityScan scanEntities(FindRequest request, Binding original, int visitLimit);
//$$     default EntityScan scanEntities(FindRequest request, Binding original, int visitLimit, BooleanSupplier withinBudget, FindLog log) {
//$$         return scanEntities(request, original, visitLimit);
//$$     }
//$$     default EntityScan scanEntities(FindRequest request, Binding original, int visitLimit, BooleanSupplier withinBudget, FindLog log, String phaseRole) {
//$$         return scanEntities(request, original, visitLimit, withinBudget, log);
//$$     }
//$$     FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z);
//$$     FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate);
//$$     default FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate, FindLog log) {
//$$         return revalidate(request, original, candidate);
//$$     }
//$$     default FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate, FindLog log, String phaseRole) {
//$$         return revalidate(request, original, candidate, log);
//$$     }
//$$ }

//#endif
