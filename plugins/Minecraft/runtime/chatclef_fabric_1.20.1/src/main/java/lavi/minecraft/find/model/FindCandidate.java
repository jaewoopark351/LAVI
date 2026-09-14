//#if MC == 12001
//$$ package lavi.minecraft.find.model;

//$$ //20260914_kpopmodder: Retain scalar candidate identity and deterministic ranking without live entities.
//$$ public record FindCandidate(int entityId, String identityDigest, String stableSortKey,
//$$                             int x, int y, int z, double distanceSquared) {
//$$     public FindCandidate {
//$$         if (identityDigest == null || stableSortKey == null || !Double.isFinite(distanceSquared)) {
//$$             throw new IllegalArgumentException("invalid_find_candidate");
//$$         }
//$$     }
//$$     @Override public String toString() { return "FindCandidate[entity=" + (entityId >= 0) + "]"; }
//$$ }

//#endif
