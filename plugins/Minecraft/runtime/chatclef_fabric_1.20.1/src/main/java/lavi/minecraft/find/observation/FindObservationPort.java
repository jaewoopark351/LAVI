//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;

//$$ //20260914_kpopmodder: Client reads are separated from finite operation state for ownership and testing.
//$$ public interface FindObservationPort {
//$$     record Binding(Object world, Object player, String dimension, double x, double y, double z, int bottomY, int topY) { }
//$$     record EntityScan(int visited, int matched, boolean complete, FindCandidate nearest) { }
//$$     Binding binding();
//$$     boolean matches(Binding original);
//$$     default boolean resourceBindingMatches(FindRequest request) { return true; }
//$$     EntityScan scanEntities(FindRequest request, Binding original, int visitLimit);
//$$     FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z);
//$$     FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate);
//$$ }

//#endif
