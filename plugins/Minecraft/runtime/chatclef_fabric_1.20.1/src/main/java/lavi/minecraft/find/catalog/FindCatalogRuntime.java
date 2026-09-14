//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import adris.altoclef.Debug;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.resource.ResourceManager;

//$$ //20260914_kpopmodder: One Fabric-owned runtime catalog is replaced atomically at client tick boundaries.
//$$ public final class FindCatalogRuntime {
//$$     private static final FindCatalogRuntime INSTANCE = new FindCatalogRuntime();
//$$     private volatile FindCatalogSnapshot snapshot;
//$$     private ResourceManager manager;
//$$     private volatile boolean invalidated = true;
//$$     private long generation;
//$$     private FindCatalogRuntime() { }
//$$     public static FindCatalogRuntime instance() { return INSTANCE; }
//$$     public FindCatalogSnapshot currentSnapshot() { return invalidated ? null : snapshot; }
//$$     public void invalidate() { invalidated = true; snapshot = null; }
//$$     public void onEndClientTick(MinecraftClient client) {
//$$         if (!client.isOnThread()) throw new IllegalStateException("client_thread_required");
//$$         ResourceManager current = client.getResourceManager();
//$$         if (!invalidated && manager == current) return;
//$$         manager = current;
//$$         invalidated = false;
//$$         if (generation == Long.MAX_VALUE) {
//$$             snapshot = FindCatalogSnapshot.incomplete(generation, "resource_generation_exhausted");
//$$             return;
//$$         }
//$$         generation++;
//$$         FindCatalogSnapshot captured;
//$$         try { captured = FindCatalogCapture.capture(client, generation); }
//$$         catch (RuntimeException failure) { captured = FindCatalogSnapshot.incomplete(generation, "runtime_catalog_capture_failed"); }
//$$         snapshot = captured;
//$$         try {
//$$             Debug.logInternal("LAVI FIND catalog generation=" + generation + " complete=" + captured.complete()
//$$                     + " records=" + captured.records().size() + " pages=" + captured.pages().size() + " reason=" + captured.reason());
//$$             if (!captured.complete()) Debug.logWarning("LAVI FIND catalog unavailable generation=" + generation + " reason=" + captured.reason());
//$$         } catch (RuntimeException | LinkageError ignored) { }
//$$     }
//$$ }

//#endif
