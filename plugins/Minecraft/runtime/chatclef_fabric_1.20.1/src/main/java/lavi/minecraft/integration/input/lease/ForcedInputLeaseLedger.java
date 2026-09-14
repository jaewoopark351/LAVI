//#if MC == 12001
//$$ package lavi.minecraft.integration.input.lease;
//$$
//$$ import baritone.api.utils.input.Input;
//$$ import java.util.EnumMap;
//$$
//$$ //20260914_kpopmodder: Exact identity claims are invalidated by every unrelated native writer.
//$$ public final class ForcedInputLeaseLedger {
//$$     private final EnumMap<Input, Object> owners = new EnumMap<>(Input.class);
//$$
//$$     public synchronized boolean claim(Input input, Object owner, boolean forcedBefore) {
//$$         if (input == null || owner == null) return false;
//$$         Object current = owners.get(input);
//$$         if (current == owner) return true;
//$$         if (current != null || forcedBefore) return false;
//$$         owners.put(input, owner);
//$$         return true;
//$$     }
//$$
//$$     public synchronized boolean owns(Input input, Object owner) {
//$$         return input != null && owner != null && owners.get(input) == owner;
//$$     }
//$$
//$$     public synchronized boolean retire(Input input, Object owner) {
//$$         if (!owns(input, owner)) return false;
//$$         owners.remove(input);
//$$         return true;
//$$     }
//$$
//$$     public synchronized void externalWrite(Input input) {
//$$         // Native HashMap permits null; metadata must not change that setter's exception behavior.
//$$         if (input != null) owners.remove(input);
//$$     }
//$$
//$$     public synchronized void externalClear() { owners.clear(); }
//$$ }
//#endif
