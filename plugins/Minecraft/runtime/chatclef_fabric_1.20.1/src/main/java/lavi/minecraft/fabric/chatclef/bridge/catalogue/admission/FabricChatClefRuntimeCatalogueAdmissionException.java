package lavi.minecraft.fabric.chatclef.bridge.catalogue.admission;

import adris.altoclef.commandsystem.CommandException;

//20260915_kpopmodder: Use the existing command failure path without inventing a second executor.
public final class FabricChatClefRuntimeCatalogueAdmissionException extends CommandException {
    public FabricChatClefRuntimeCatalogueAdmissionException(String reason) { super(reason); }
}
