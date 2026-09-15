//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue.lifecycle;

import lavi.minecraft.fabric.chatclef.bridge.catalogue.FabricChatClefCatalogueCapture;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

//20260915_kpopmodder: Resource lifecycle adapter only invalidates the main-thread catalogue capture.
public final class FabricChatClefCatalogueReloadListener implements SimpleSynchronousResourceReloadListener {
    private final FabricChatClefCatalogueCapture capture;
    public FabricChatClefCatalogueReloadListener(FabricChatClefCatalogueCapture capture) { this.capture = capture; }
    @Override public Identifier getFabricId() {
        //#if MC >= 12100
        return Identifier.of("chatclef", "lavi_korean_command_catalogue");
        //#else
        //$$ return new Identifier("chatclef", "lavi_korean_command_catalogue");
        //#endif
    }
    @Override public void reload(ResourceManager manager) { capture.invalidate(); }
}
//#endif
