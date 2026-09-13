//#if MC == 12001
package lavi.minecraft.integration.lifecycle.root;

//20260913_kpopmodder: Freeze the existing user-chain completion boundary without declaring goal success.
public record UserRootCompletion(boolean stopStateAvailable, boolean stopped, String error, String miningToolFailureReason) {
    public static UserRootCompletion known(boolean stopped) {
        return known(stopped, "");
    }
    public static UserRootCompletion known(boolean stopped, String miningToolFailureReason) {
        return new UserRootCompletion(true, stopped, "", miningToolFailureReason);
    }
    public static UserRootCompletion unavailable(String error) {
        return new UserRootCompletion(false, false, error, "");
    }
}
//#endif
