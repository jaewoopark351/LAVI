//#if MC == 12001
//$$ package lavi.minecraft.find.approach.policy;
//$$
//$$ //20260914_kpopmodder: Conservative supported-kind distances are explicit and never universal mod safety.
//$$ public record FindApproachEnvelope(double minimum, double maximum) {
//$$     public FindApproachEnvelope {
//$$         if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum < 0 || maximum <= minimum) {
//$$             throw new IllegalArgumentException("invalid_find_approach_envelope");
//$$         }
//$$     }
//$$     public boolean contains(double distanceSquared) {
//$$         return Double.isFinite(distanceSquared) && distanceSquared >= minimum * minimum
//$$                 && distanceSquared <= maximum * maximum;
//$$     }
//$$     public static FindApproachEnvelope forKind(String kind, String canonicalId, boolean passive) {
//$$         if ("block".equals(kind)) return new FindApproachEnvelope(1.5, 3.5);
//$$         if ("player".equals(kind)) return new FindApproachEnvelope(4.5, 6.5);
//$$         if (!"entity".equals(kind) || canonicalId == null || !canonicalId.startsWith("minecraft:")) return null;
//$$         if ("minecraft:creeper".equals(canonicalId)) return new FindApproachEnvelope(12, 16);
//$$         if (passive) {
//$$             return switch (canonicalId) {
//$$                 case "minecraft:villager", "minecraft:wandering_trader", "minecraft:cow", "minecraft:mooshroom",
//$$                         "minecraft:sheep", "minecraft:pig", "minecraft:chicken", "minecraft:rabbit",
//$$                         "minecraft:horse", "minecraft:donkey", "minecraft:mule", "minecraft:cat",
//$$                         "minecraft:ocelot", "minecraft:turtle" -> new FindApproachEnvelope(2.5, 4.5);
//$$                 default -> null;
//$$             };
//$$         }
//$$         // Unknown threat ranges, bosses and ranged attackers need an independently validated profile.
//$$         return switch (canonicalId) {
//$$             case "minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:spider",
//$$                     "minecraft:cave_spider", "minecraft:silverfish", "minecraft:endermite" -> new FindApproachEnvelope(8, 12);
//$$             default -> null;
//$$         };
//$$     }
//$$ }
//#endif
