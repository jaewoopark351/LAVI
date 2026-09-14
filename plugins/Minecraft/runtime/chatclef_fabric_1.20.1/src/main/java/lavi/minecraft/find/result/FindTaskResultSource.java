//#if MC == 12001
//$$ package lavi.minecraft.find.result;

//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import lavi.minecraft.integration.lifecycle.root.UserRootLifetime;

//$$ //20260914_kpopmodder: Bridge projection observes the authoritative FIND task without owning its behavior.
//$$ public interface FindTaskResultSource {
//$$     FindRequest request();
//$$     String operationId();
//$$     FindOutcome outcome();
//$$     default void bindUserRootLifetime(UserRootLifetime lifetime) { }
//$$     default void observeClientTick() { }
//$$     default void retireOwnedResources(String reason) { }
//$$     void diagnosticRetired(String reason);
//$$     void suppressNativePresentation();
//$$     boolean nativePresentationSuppressed();
//$$ }

//#endif
