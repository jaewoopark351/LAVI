//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import java.util.function.BooleanSupplier;
//$$ import java.util.function.Supplier;
//$$ import adris.altoclef.AltoClef;
//$$ import adris.altoclef.trackers.EntityTracker;
//$$ import lavi.minecraft.find.catalog.FindResourceBinding;
//$$ import lavi.minecraft.find.diagnostics.FindLog;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindIdentityDigest;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.block.MinecraftFindBlockObservation;
//$$ import lavi.minecraft.find.observation.entity.MinecraftFindEntityObservation;
//$$ import net.minecraft.client.MinecraftClient;

//$$ //20260914_kpopmodder: Own client binding and delegate independent entity/block observation implementations.
//$$ public final class MinecraftFindObservationPort implements FindObservationPort {
//$$     private final MinecraftClient client;
//$$     private final MinecraftFindEntityObservation entities;
//$$     private final MinecraftFindBlockObservation blocks = new MinecraftFindBlockObservation();
//$$     public MinecraftFindObservationPort(MinecraftClient client) {
//$$         this(client, () -> AltoClef.getInstance().getEntityTracker());
//$$     }
//$$     public MinecraftFindObservationPort(MinecraftClient client, Supplier<EntityTracker> tracker) {
//$$         this.client = client; entities = new MinecraftFindEntityObservation(client, tracker);
//$$     }
//$$     private void requireThread() {
//$$         if (!client.isOnThread()) throw new IllegalStateException("client_thread_required");
//$$     }
//$$     @Override public Binding binding() {
//$$         requireThread();
//$$         if (client.world == null || client.player == null) return null;
//$$         return new Binding(client.world, client.player, client.world.getRegistryKey().getValue().toString(),
//$$                 client.player.getX(), client.player.getY(), client.player.getZ(), client.world.getBottomY(), client.world.getTopY());
//$$     }
//$$     @Override public boolean matches(Binding original) {
//$$         requireThread();
//$$         return original != null && original.world() == client.world && original.player() == client.player
//$$                 && client.world != null && original.dimension().equals(client.world.getRegistryKey().getValue().toString());
//$$     }
//$$     @Override public boolean resourceBindingMatches(FindRequest request) {
//$$         requireThread();
//$$         return FindResourceBinding.currentMatches(request);
//$$     }
//$$     @Override public EntityScan scanEntities(FindRequest request, Binding original, int limit) {
//$$         requireThread();
//$$         return entities.scan(request, original, limit, () -> true, null);
//$$     }
//$$     @Override public EntityScan scanEntities(FindRequest request, Binding original, int limit, BooleanSupplier withinBudget, FindLog log) {
//$$         requireThread();
//$$         return entities.scan(request, original, limit, withinBudget, log);
//$$     }
//$$     @Override public EntityScan scanEntities(FindRequest request, Binding original, int limit, BooleanSupplier withinBudget, FindLog log, String phaseRole) {
//$$         requireThread();
//$$         return entities.scan(request, original, limit, withinBudget, log, phaseRole);
//$$     }
//$$     @Override public FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z) {
//$$         requireThread();
//$$         return blocks.read(request, original, x, y, z);
//$$     }
//$$     @Override public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate) {
//$$         return revalidate(request, original, candidate, null);
//$$     }
//$$     @Override public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate, FindLog log) {
//$$         requireThread();
//$$         if (!matches(original)) return null;
//$$         if (request.kind().equals("block")) return readBlock(request, original, candidate.x(), candidate.y(), candidate.z());
//$$         return entities.revalidate(request, original, candidate, log);
//$$     }
//$$     @Override public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate, FindLog log, String phaseRole) {
//$$         requireThread();
//$$         if (!matches(original)) return null;
//$$         if (request.kind().equals("block")) return readBlock(request, original, candidate.x(), candidate.y(), candidate.z());
//$$         return entities.revalidate(request, original, candidate, log, phaseRole);
//$$     }
//$$     public static String digest(String input) {
//$$         return FindIdentityDigest.digest(input);
//$$     }
//$$ }

//#endif
