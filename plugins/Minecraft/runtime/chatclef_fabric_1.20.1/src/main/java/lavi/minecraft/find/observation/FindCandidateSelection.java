//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import lavi.minecraft.find.model.FindCandidate;

//$$ //20260914_kpopmodder: Candidate ranking uses frozen-origin distance and stable UUID or XYZ ties.
//$$ public final class FindCandidateSelection {
//$$     private FindCandidateSelection() { }
//$$     public static FindCandidate nearer(FindCandidate first, FindCandidate second) {
//$$         if (second == null) return first;
//$$         if (first == null) return second;
//$$         int distance = Double.compare(first.distanceSquared(), second.distanceSquared());
//$$         if (distance != 0) return distance < 0 ? first : second;
//$$         if (first.entityId() >= 0) {
//$$             return first.stableSortKey().compareTo(second.stableSortKey()) <= 0 ? first : second;
//$$         }
//$$         int comparison = Integer.compare(first.x(), second.x());
//$$         if (comparison == 0) comparison = Integer.compare(first.y(), second.y());
//$$         if (comparison == 0) comparison = Integer.compare(first.z(), second.z());
//$$         return comparison <= 0 ? first : second;
//$$     }
//$$ }

//#endif
