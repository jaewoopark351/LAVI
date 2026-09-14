//#if MC == 12001
//$$ package lavi.minecraft.find.approach.input;
//$$
//$$ import java.util.EnumSet;
//$$ import java.util.Map;
//$$ import baritone.api.utils.input.Input;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.integration.input.lease.ForcedInputLeaseChannel;
//$$
//$$ //20260914_kpopmodder: Stage native requests and release only exact per-handler leases.
//$$ public final class FindOwnedMovementInputs {
//$$     private static final EnumSet<Input> ALLOWED = EnumSet.of(Input.MOVE_FORWARD, Input.MOVE_BACK,
//$$             Input.MOVE_LEFT, Input.MOVE_RIGHT, Input.SPRINT);
//$$     private final ForcedInputLeaseChannel channel;
//$$     private final Object owner = new Object();
//$$     private final EnumSet<Input> acquired = EnumSet.noneOf(Input.class);
//$$     private final FindLog log;
//$$
//$$     public FindOwnedMovementInputs(ForcedInputLeaseChannel channel, FindLog log) {
//$$         this.channel = channel; this.log = log;
//$$     }
//$$     public boolean apply(Map<Input, Boolean> staged) {
//$$         if (channel == null || hasForbiddenInput(staged)) { release(); return false; }
//$$         for (Input input : acquired) {
//$$             if (!channel.lavi$ownsForcedInput(input, owner)) {
//$$                 log.event("INPUT_LEASE_LOST", "input", input.name());
//$$                 release(); return false;
//$$             }
//$$         }
//$$         // Claim before writing; a failed multi-key acquisition releases only this owner's earlier claims.
//$$         for (Map.Entry<Input, Boolean> entry : staged.entrySet()) {
//$$             if (!Boolean.TRUE.equals(entry.getValue())) continue;
//$$             Input input = entry.getKey();
//$$             if (!acquired.contains(input)) {
//$$                 boolean claimed = channel.lavi$claimForcedInput(input, owner);
//$$                 log.event("INPUT_CLAIM", "input", input.name(), "claimed", claimed);
//$$                 if (!claimed) { release(); return false; }
//$$                 acquired.add(input);
//$$             }
//$$         }
//$$         for (Input input : acquired) {
//$$             if (!channel.lavi$writeForcedInput(input, owner, Boolean.TRUE.equals(staged.get(input)))) {
//$$                 log.event("INPUT_WRITE_REJECTED", "input", input.name());
//$$                 release(); return false;
//$$             }
//$$         }
//$$         return true;
//$$     }
//$$     public void release() {
//$$         var iterator = acquired.iterator();
//$$         while (iterator.hasNext()) {
//$$             Input input = iterator.next();
//$$             boolean released = channel != null && channel.lavi$releaseForcedInput(input, owner);
//$$             boolean stillOwned = channel != null && channel.lavi$ownsForcedInput(input, owner);
//$$             log.event("INPUT_RELEASE", "input", input.name(), "released", released,
//$$                     "supersededWriterPreserved", !released && !stillOwned, "leaseStillOwned", stillOwned);
//$$             if (released || !stillOwned) iterator.remove();
//$$         }
//$$     }
//$$     public boolean quiet() {
//$$         return acquired.isEmpty();
//$$     }
//$$     public boolean owns(Input input) {
//$$         return acquired.contains(input) && channel != null && channel.lavi$ownsForcedInput(input, owner);
//$$     }
//$$     public static boolean hasForbiddenInput(Map<Input, Boolean> staged) {
//$$         if (staged == null) return true;
//$$         for (Map.Entry<Input, Boolean> entry : staged.entrySet()) {
//$$             if (entry.getKey() == null || entry.getValue() == null) return true;
//$$             if (entry.getValue() && !ALLOWED.contains(entry.getKey())) return true;
//$$         }
//$$         return false;
//$$     }
//$$ }
//#endif
