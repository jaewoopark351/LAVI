//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import lavi.minecraft.find.model.FindRequest;

//$$ //20260914_kpopmodder: Interpretive resource evidence must still match at observation and final publication.
//$$ public final class FindResourceBinding {
//$$     private FindResourceBinding() { }
//$$     public static boolean currentMatches(FindRequest request) {
//$$         if (request == null) return false;
//$$         if (request.kind().equals("player") || request.catalogDigest().isEmpty()) return true;
//$$         return matches(request, FindCatalogRuntime.instance().currentSnapshot());
//$$     }
//$$     static boolean matches(FindRequest request, FindCatalogSnapshot snapshot) {
//$$         return snapshot != null && snapshot.complete() && snapshot.resourceGeneration() == request.resourceGeneration()
//$$                 && snapshot.digest().equals(request.catalogDigest());
//$$     }
//$$ }
//#endif
