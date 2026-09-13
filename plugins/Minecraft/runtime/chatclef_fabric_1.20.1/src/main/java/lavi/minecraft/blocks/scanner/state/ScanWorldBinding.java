//#if MC == 12001
package lavi.minecraft.blocks.scanner.state;

//20260913_kpopmodder: A run keeps exact world/player lifetime identities and its own content-independent generation.
public record ScanWorldBinding(Object world, Object player, Object dimension, long generation) {
    public boolean matches(Object candidateWorld, Object candidatePlayer, Object candidateDimension, long candidateGeneration) {
        return world == candidateWorld && player == candidatePlayer
                && java.util.Objects.equals(dimension, candidateDimension) && generation == candidateGeneration;
    }
}
//#endif
