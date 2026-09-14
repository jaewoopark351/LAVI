//#if MC == 12001
//$$ package lavi.minecraft.find.observation;

//$$ //20260914_kpopmodder: The fixed coordinate cursor visits at most 65 cubed positions without loading chunks.
//$$ public final class FindBlockObservationCursor {
//$$     public record Position(int x, int y, int z) { }
//$$     private final int minimumX, minimumY, minimumZ, maximumX, maximumY, maximumZ;
//$$     private int x, y, z;
//$$     private boolean finished;

//$$     public FindBlockObservationCursor(double originX, double originY, double originZ, int bottomY, int topY) {
//$$         minimumX = (int) Math.floor(originX) - 32; maximumX = minimumX + 64;
//$$         minimumZ = (int) Math.floor(originZ) - 32; maximumZ = minimumZ + 64;
//$$         minimumY = Math.max(bottomY, (int) Math.floor(originY) - 32);
//$$         maximumY = Math.min(topY - 1, (int) Math.floor(originY) + 32);
//$$         x = minimumX; y = minimumY; z = minimumZ;
//$$         finished = minimumY > maximumY;
//$$     }
//$$     public boolean complete() { return finished; }
//$$     public Position next() {
//$$         if (finished) return null;
//$$         Position result = new Position(x, y, z);
//$$         if (++z > maximumZ) {
//$$             z = minimumZ;
//$$             if (++y > maximumY) {
//$$                 y = minimumY;
//$$                 if (++x > maximumX) finished = true;
//$$             }
//$$         }
//$$         return result;
//$$     }
//$$ }

//#endif
