//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ import java.nio.charset.StandardCharsets;
//$$ import java.security.MessageDigest;
//$$ import java.security.NoSuchAlgorithmException;
//$$ import java.util.HexFormat;
//$$ import lavi.minecraft.find.catalog.FindResourceBinding;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.entity.ItemEntity;
//$$ import net.minecraft.entity.mob.MobEntity;
//$$ import net.minecraft.entity.player.PlayerEntity;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.util.math.BlockPos;

//$$ //20260914_kpopmodder: Read only loaded client state; caches, path accessibility and landing are not discovery filters.
//$$ public final class MinecraftFindObservationPort implements FindObservationPort {
//$$     private final MinecraftClient client;
//$$     public MinecraftFindObservationPort(MinecraftClient client) { this.client = client; }
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
//$$         int visited = 0, matched = 0;
//$$         FindCandidate nearest = null;
//$$         for (Entity entity : ((ClientWorld) original.world()).getEntities()) {
//$$             if (visited == limit) return new EntityScan(visited, matched, false, nearest);
//$$             visited++;
//$$             if (entity == client.player || !eligible(request, entity)) continue;
//$$             double distance = distance(original, entity.getX(), entity.getY(), entity.getZ());
//$$             if (distance > 64.0 * 64.0) continue;
//$$             matched++;
//$$             nearest = FindCandidateSelection.nearer(nearest, entityCandidate(entity, distance));
//$$         }
//$$         return new EntityScan(visited, matched, true, nearest);
//$$     }
//$$     private boolean eligible(FindRequest request, Entity entity) {
//$$         if (!entity.isAlive() || entity.isRemoved()) return false;
//$$         return switch (request.kind()) {
//$$             case "entity" -> entity instanceof MobEntity && Registries.ENTITY_TYPE.getId(entity.getType()).toString().equals(request.target());
//$$             case "player" -> entity instanceof PlayerEntity && entity.getName().getString().equals(request.playerName());
//$$             case "item" -> entity instanceof ItemEntity item && !item.getStack().isEmpty()
//$$                     && Registries.ITEM.getId(item.getStack().getItem()).toString().equals(request.target());
//$$             default -> false;
//$$         };
//$$     }
//$$     @Override public FindCandidate readBlock(FindRequest request, Binding original, int x, int y, int z) {
//$$         requireThread();
//$$         double distance = distance(original, x, y, z);
//$$         if (distance > 32.0 * 32.0) return null;
//$$         ClientWorld world = (ClientWorld) original.world();
//$$         if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return null;
//$$         BlockPos position = new BlockPos(x, y, z);
//$$         if (!Registries.BLOCK.getId(world.getBlockState(position).getBlock()).toString().equals(request.target())) return null;
//$$         return new FindCandidate(-1, digest(original.dimension() + "\t" + x + "\t" + y + "\t" + z), "", x, y, z, distance);
//$$     }
//$$     @Override public FindCandidate revalidate(FindRequest request, Binding original, FindCandidate candidate) {
//$$         requireThread();
//$$         if (!matches(original)) return null;
//$$         if (request.kind().equals("block")) return readBlock(request, original, candidate.x(), candidate.y(), candidate.z());
//$$         Entity entity = ((ClientWorld) original.world()).getEntityById(candidate.entityId());
//$$         if (entity == null || entity == client.player || !eligible(request, entity)
//$$                 || !entity.getUuid().toString().equals(candidate.stableSortKey())) return null;
//$$         double distance = distance(original, entity.getX(), entity.getY(), entity.getZ());
//$$         if (distance > 64.0 * 64.0) return null;
//$$         return entityCandidate(entity, distance);
//$$     }
//$$     private FindCandidate entityCandidate(Entity entity, double distance) {
//$$         String uuid = entity.getUuid().toString();
//$$         BlockPos position = entity.getBlockPos();
//$$         return new FindCandidate(entity.getId(), digest(uuid), uuid, position.getX(), position.getY(), position.getZ(), distance);
//$$     }
//$$     private static double distance(Binding original, double x, double y, double z) {
//$$         double dx = x - original.x(), dy = y - original.y(), dz = z - original.z();
//$$         return dx * dx + dy * dy + dz * dz;
//$$     }
//$$     public static String digest(String input) {
//$$         try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
//$$         catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("sha256_unavailable", impossible); }
//$$     }
//$$ }

//#endif
