package lavi.minecraft.diagnostics.baritone.builder;

//20260913_kpopmodder: Keep compact generations, path/count/index facts in each bounded history item.
public final class BuilderTransitionSummary {
    private BuilderTransitionSummary() { }
    public static String format(String event, String reason, Object[] fields) {
        StringBuilder text = new StringBuilder(reason);
        for (int i = 0; fields != null && i + 1 < fields.length; i += 2) {
            String key = String.valueOf(fields[i]);
            if (!(key.endsWith("PathIdentity") || key.endsWith("PositionsCount") || key.endsWith("MovementsCount")
                    || key.endsWith("Generation") || key.endsWith("Position") || key.endsWith("Index")
                    || key.equals("movementsCountAtAccess"))) continue;
            String abbreviated = key.replace("PathIdentity", "P").replace("PositionsCount", "PC")
                    .replace("MovementsCount", "MC").replace("calculationGeneration", "G")
                    .replace("Position", "I").replace("Generation", "G").replace("movementsCountAtAccess", "MC");
            String entry = " " + abbreviated + '=' + fields[i + 1];
            if (text.length() + entry.length() > 240) { text.append(" [truncated]"); break; }
            text.append(entry);
        }
        return text.toString();
    }
}
