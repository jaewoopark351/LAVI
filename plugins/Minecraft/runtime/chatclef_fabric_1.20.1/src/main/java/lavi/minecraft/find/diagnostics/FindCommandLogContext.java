//#if MC == 12001
//$$ package lavi.minecraft.find.diagnostics;

//$$ import java.util.ArrayList;
//$$ import java.util.List;
//$$ import java.util.Set;
//$$ import lavi.minecraft.diagnostics.ChatClefDiagnostics;

//$$ //20260914_kpopmodder: Keep operation correlation without copying raw command text or player names into FIND records.
//$$ final class FindCommandLogContext {
//$$     private static final Set<String> ALLOWED = Set.of("commandContextAvailable", "commandRequestId",
//$$             "commandCorrelationId", "commandSessionId", "commandConnectionGeneration", "commandSource", "commandContextError");
//$$     private FindCommandLogContext() { }
//$$     static Object[] capture() { return filter(ChatClefDiagnostics.currentCommandContextFields()); }
//$$     static Object[] filter(Object[] source) {
//$$         if (source == null) return new Object[0];
//$$         List<Object> fields = new ArrayList<>(ALLOWED.size() * 2);
//$$         Set<String> captured = new java.util.HashSet<>();
//$$         for (int index = 0; index + 1 < source.length; index += 2) {
//$$             if (!(source[index] instanceof String key) || !ALLOWED.contains(key) || captured.contains(key)) continue;
//$$             Object value = source[index + 1];
//$$             if (key.equals("commandContextAvailable")) {
//$$                 if (!(value instanceof Boolean)) continue;
//$$             } else {
//$$                 if (!(value instanceof String text)) continue;
//$$                 int length = text.codePointCount(0, text.length());
//$$                 value = length <= 128 ? text : text.substring(0, text.offsetByCodePoints(0, 128));
//$$             }
//$$             captured.add(key); fields.add(key); fields.add(value);
//$$         }
//$$         return fields.toArray();
//$$     }
//$$ }
//#endif
