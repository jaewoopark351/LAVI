//#if MC == 12001
//$$ package lavi.minecraft.find.observation.block;
//$$
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindIdentityDigest;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.find.observation.FindObservationPort.Binding;
//$$ import net.minecraft.client.world.ClientWorld;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.util.math.BlockPos;
//$$
//$$ //20260914_kpopmodder: Preserve bounded loaded-block reads independently of entity observation.
//$$ public final class MinecraftFindBlockObservation {
//$$     public FindCandidate read(FindRequest request, Binding original, int x, int y, int z) {
//$$         double dx = x - original.x(), dy = y - original.y(), dz = z - original.z();
//$$         double distance = dx * dx + dy * dy + dz * dz;
//$$         if (distance > 32.0 * 32.0) return null;
//$$         ClientWorld world = (ClientWorld) original.world();
//$$         if (!world.getChunkManager().isChunkLoaded(x >> 4, z >> 4)) return null;
//$$         BlockPos position = new BlockPos(x, y, z);
//$$         if (!Registries.BLOCK.getId(world.getBlockState(position).getBlock()).toString().equals(request.target())) return null;
//$$         return new FindCandidate(-1, FindIdentityDigest.digest(original.dimension() + "\t" + x + "\t" + y + "\t" + z), "", x, y, z, distance);
//$$     }
//$$ }
//#endif
