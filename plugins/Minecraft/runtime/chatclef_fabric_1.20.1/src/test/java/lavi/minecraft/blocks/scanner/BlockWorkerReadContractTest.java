package lavi.minecraft.blocks.scanner;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.FieldInsnNode;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Production bytecode must not reintroduce worker-side world reads or lazy tracker updates.
class BlockWorkerReadContractTest {
    @Test void workerCompletionCanOnlyOfferMailboxDataAndCannotPublishOrReadGameIdentities() throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader("adris.altoclef.commands.BlockScanner").accept(node, 0);
        List<MethodNode> workers = node.methods.stream().filter(method -> method.name.startsWith("lambda$tick$")).toList();
        assertEquals(1, workers.size());
        boolean offered = false;
        for (var instruction : workers.get(0).instructions) {
            if (instruction instanceof MethodInsnNode call) {
                assertFalse(List.of("getWorld", "getPlayer", "isCurrentRun", "commitClientCompletion", "putAll", "clear").contains(call.name), call.owner + "." + call.name);
            }
            if (instruction instanceof FieldInsnNode field) {
                assertFalse(List.of("scannedBlocks", "scannedChunks", "cachedScannedBlocks", "scanWorld", "scanPlayer").contains(field.name), field.name);
                if (field.name.equals("completedRun")) offered = true;
                if (field.name.equals("activeRun")) assertNotEquals(org.objectweb.asm.Opcodes.PUTFIELD, field.getOpcode(), "Worker cannot retire current client ownership");
            }
        }
        assertTrue(offered);
    }
    @Test void pathWorkerPredicateOnlyReadsThePublishedImmutableValue() throws IOException {
        MethodNode predicate = method("adris.altoclef.trackers.UserBlockRangeTracker", "isNearUserTrackedBlock");
        int snapshotReads = 0;
        for (var instruction : predicate.instructions) if (instruction instanceof MethodInsnNode call) {
            assertFalse(List.of("ensureUpdated", "updateState", "getWorld", "getBlockState", "getPriority").contains(call.name), call.owner + "." + call.name);
            if (call.name.equals("snapshot")) snapshotReads++;
        }
        assertEquals(1, snapshotReads);
    }
    @Test void scannerWorkerConsumesSnapshotsWithoutQueryingLiveWorldOrPlayer() throws IOException {
        for (String name : List.of("rescan", "scanChunk")) {
            MethodNode worker = method("adris.altoclef.commands.BlockScanner", name);
            for (var instruction : worker.instructions) if (instruction instanceof MethodInsnNode call) {
                assertFalse(List.of("getWorld", "getPlayer", "getBlockState", "getChunk", "getChunkManager", "isUnreachable").contains(call.name), name + " -> " + call.owner + "." + call.name);
            }
        }
    }
    private static MethodNode method(String type, String name) throws IOException {
        ClassNode node = new ClassNode();
        new ClassReader(type).accept(node, 0);
        return node.methods.stream().filter(method -> method.name.equals(name)).findFirst().orElseThrow();
    }
}
