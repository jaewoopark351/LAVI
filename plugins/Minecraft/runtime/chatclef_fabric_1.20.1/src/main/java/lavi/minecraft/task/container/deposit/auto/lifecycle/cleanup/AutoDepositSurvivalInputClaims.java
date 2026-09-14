package lavi.minecraft.task.container.deposit.auto.lifecycle.cleanup;

//20260914_kpopmodder: Freeze only inputs explicitly claimed by already-evaluated survival behavior.
/** Null means unclaimed, not a request to release an input. */
record AutoDepositSurvivalInputClaims(Boolean sneakPressed, Boolean usePressed, Boolean attackForced) {
}
