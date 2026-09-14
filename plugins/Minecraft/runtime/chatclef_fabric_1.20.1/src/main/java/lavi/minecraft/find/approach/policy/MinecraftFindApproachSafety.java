//#if MC == 12001
//$$ package lavi.minecraft.find.approach.policy;
//$$
//$$ import java.util.Set;
//$$ import adris.altoclef.AltoClef;
//$$ import baritone.Baritone;
//$$ import baritone.api.utils.input.Input;
//$$ import lavi.minecraft.find.model.FindCandidate;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import net.minecraft.block.Block;
//$$ import net.minecraft.block.Blocks;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.entity.Entity;
//$$ import net.minecraft.entity.passive.PassiveEntity;
//$$ import net.minecraft.util.math.BlockPos;
//$$ import net.minecraft.util.math.Vec3d;
//$$
//$$ //20260914_kpopmodder: Proven dry flat cells exclude interaction, fluid, unsupported surfaces and edge gaps.
//$$ public final class MinecraftFindApproachSafety {
//$$     private static final Set<Block> SAFE_FLOORS = Set.of(Blocks.STONE, Blocks.COBBLESTONE, Blocks.DIRT,
//$$             Blocks.GRASS_BLOCK, Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.GRANITE, Blocks.DIORITE,
//$$             Blocks.ANDESITE, Blocks.STONE_BRICKS, Blocks.SANDSTONE, Blocks.OAK_PLANKS, Blocks.SPRUCE_PLANKS,
//$$             Blocks.BIRCH_PLANKS, Blocks.JUNGLE_PLANKS, Blocks.ACACIA_PLANKS, Blocks.DARK_OAK_PLANKS,
//$$             Blocks.BRICKS, Blocks.NETHERRACK, Blocks.END_STONE);
//$$     private MinecraftFindApproachSafety() { }
//$$
//$$     public static boolean flatCell(MinecraftClient client, BlockPos feet) {
//$$         if (client.world == null || !client.world.getChunkManager().isChunkLoaded(feet.getX() >> 4, feet.getZ() >> 4)
//$$                 || !client.world.getWorldBorder().contains(feet)) return false;
//$$         if (!SAFE_FLOORS.contains(client.world.getBlockState(feet.down()).getBlock())) return false;
//$$         // AIR-only body excludes doors, plants, vines, ladders, pressure plates and arbitrary mod callbacks.
//$$         return client.world.getBlockState(feet).isAir() && client.world.getBlockState(feet.up()).isAir()
//$$                 && client.world.getBlockState(feet.up(2)).isAir();
//$$     }
//$$     public static boolean footprint(MinecraftClient client, BlockPos source, BlockPos destination) {
//$$         if (source.getY() != destination.getY() || Math.abs(source.getX() - destination.getX()) > 1
//$$                 || Math.abs(source.getZ() - destination.getZ()) > 1) return false;
//$$         for (int x = Math.min(source.getX(), destination.getX()) - 1; x <= Math.max(source.getX(), destination.getX()) + 1; x++) {
//$$             for (int z = Math.min(source.getZ(), destination.getZ()) - 1; z <= Math.max(source.getZ(), destination.getZ()) + 1; z++) {
//$$                 if (!flatCell(client, new BlockPos(x, source.getY(), z))) return false;
//$$             }
//$$         }
//$$         return true;
//$$     }
//$$     public static boolean nativeFootprintMatches(MinecraftClient client, AltoClef mod, BlockPos source, BlockPos destination) {
//$$         var baritone = mod.getClientBaritone();
//$$         if (Boolean.TRUE.equals(Baritone.settings().pathThroughCachedOnly.value) || baritone.bsi == null
//$$                 || baritone.getPlayerContext().world() != client.world || baritone.getPlayerContext().player() != client.player)
//$$             return false;
//$$         // Native prepared/helpers must see the same AIR and supported floor already validated in the live world.
//$$         // This also rejects an old native BSI snapshot whose cached/world reads disagree before any tool callback.
//$$         for (int x = Math.min(source.getX(), destination.getX()) - 1; x <= Math.max(source.getX(), destination.getX()) + 1; x++) {
//$$             for (int z = Math.min(source.getZ(), destination.getZ()) - 1; z <= Math.max(source.getZ(), destination.getZ()) + 1; z++) {
//$$                 for (int y = source.getY() - 1; y <= source.getY() + 2; y++) {
//$$                     BlockPos position = new BlockPos(x, y, z);
//$$                     if (!client.world.getBlockState(position).equals(baritone.bsi.get0(position))) return false;
//$$                 }
//$$             }
//$$         }
//$$         return true;
//$$     }
//$$     public static Vec3d target(MinecraftClient client, FindRequest request, FindCandidate candidate) {
//$$         if (request.kind().equals("block")) return new Vec3d(candidate.x() + .5, candidate.y() + .5, candidate.z() + .5);
//$$         Entity entity = client.world.getEntityById(candidate.entityId());
//$$         return entity != null && entity.getUuid().toString().equals(candidate.stableSortKey()) ? entity.getPos() : null;
//$$     }
//$$     public static FindApproachEnvelope envelope(MinecraftClient client, FindRequest request, FindCandidate candidate) {
//$$         Entity entity = candidate.entityId() < 0 ? null : client.world.getEntityById(candidate.entityId());
//$$         return FindApproachEnvelope.forKind(request.kind(), request.canonicalTargetId(), entity instanceof PassiveEntity);
//$$     }
//$$     public static boolean contended(AltoClef mod) {
//$$         if (mod == null || mod.getPlayer() == null || mod.getInputControls() == null || mod.getClientBaritone() == null) return true;
//$$         if (mod.getExtraBaritoneSettings().isInteractionPaused()
//$$                 || mod.getMobDefenseChain().isToolInputClaimed() || mod.getMobDefenseChain().isShielding()
//$$                 || mod.getFoodChain().isTryingToEat() || mod.getMLGBucketChain().isChorusFruiting()) return true;
//$$         var baritone = mod.getClientBaritone();
//$$         if (baritone.getPathingBehavior().getCurrent() != null || baritone.getPathingBehavior().getNext() != null
//$$                 || baritone.getPathingBehavior().getInProgress().isPresent() || baritone.getPathingBehavior().getGoal() != null
//$$                 || baritone.getPathingControlManager().mostRecentInControl().isPresent()) return true;
//$$         for (Input input : new Input[]{Input.CLICK_LEFT, Input.CLICK_RIGHT, Input.JUMP, Input.SNEAK}) {
//$$             if (mod.getInputControls().isHeldDown(input) || baritone.getInputOverrideHandler().isInputForcedDown(input)) return true;
//$$         }
//$$         return mod.getControllerExtras().isBreakingBlock();
//$$     }
//$$ }
//#endif
