package lavi.minecraft.diagnostics.toolselect;

import adris.altoclef.AltoClef;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;

import java.util.StringJoiner;

//20260801_kpopmodder: Observe the actual StorageHelper best-tool filtering boundary without changing selection behavior.
public final class BestToolSlotDiagnostics {
    public static final String DECISION_HARDNESS_ZERO_USE_EQUIP_SLOT = "HARDNESS_ZERO_USE_EQUIP_SLOT";
    public static final String DECISION_NO_ELIGIBLE_TOOL = "NO_ELIGIBLE_TOOL";
    public static final String DECISION_SELECTED_TOOL = "SELECTED_TOOL";

    private static final int MAX_CANDIDATES = 12;
    private static String lastBestToolSlotFingerprint = "";

    private BestToolSlotDiagnostics() {
    }

    public static Scan start(BlockState targetState) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return Scan.disabled();
        }
        return new Scan(true, targetState);
    }

    public static final class Scan {
        private final boolean enabled;
        private final BlockState targetState;
        private final StringJoiner candidates = new StringJoiner(",", "[", "]");
        private int candidateCount;
        private int emittedCandidates;
        private int toolCandidateCount;
        private int defaultSuitableToolCount;
        private int savedSuitableToolCount;
        private int eligibleToolCount;
        private int shearsCandidateCount;
        private int effectiveShearsCount;

        private Scan(boolean enabled, BlockState targetState) {
            this.enabled = enabled;
            this.targetState = targetState;
        }

        private static Scan disabled() {
            return new Scan(false, null);
        }

        public void observeToolCandidate(AltoClef mod,
                                         Slot slot,
                                         ItemStack stack,
                                         boolean defaultSuitable,
                                         boolean shouldSave,
                                         double speed,
                                         boolean becameBest) {
            if (!enabled) {
                return;
            }
            candidateCount++;
            toolCandidateCount++;
            if (defaultSuitable) {
                defaultSuitableToolCount++;
            }
            if (defaultSuitable && shouldSave) {
                savedSuitableToolCount++;
            }
            if (defaultSuitable && !shouldSave) {
                eligibleToolCount++;
            }
            if (emittedCandidates >= MAX_CANDIDATES) {
                return;
            }

            String outcome = toolOutcome(defaultSuitable, shouldSave, becameBest);
            String saveDecision = defaultSuitable
                    ? saveDecision(mod, stack, targetState, shouldSave)
                    : "not_evaluated#reason=NOT_DEFAULT_SUITABLE";
            candidates.add(ChatClefDiagnostics.slotSummary(slot)
                    + "#stack=" + stackDetails(stack)
                    + "#defaultStackSuitable=" + defaultSuitable
                    + "#shouldSave=" + (defaultSuitable ? Boolean.toString(shouldSave) : "not_evaluated")
                    + "#selectionOutcome=" + outcome
                    + "#saveDecision=" + saveDecision
                    + "#speed=" + speedValue(speed));
            emittedCandidates++;
        }

        public void observeShearsCandidate(Slot slot, ItemStack stack, boolean effective, boolean selected) {
            if (!enabled) {
                return;
            }
            candidateCount++;
            shearsCandidateCount++;
            if (effective) {
                effectiveShearsCount++;
            }
            if (emittedCandidates >= MAX_CANDIDATES) {
                return;
            }
            candidates.add(ChatClefDiagnostics.slotSummary(slot)
                    + "#stack=" + stackDetails(stack)
                    + "#selectionOutcome=" + (effective ? (selected ? "SHEARS_SELECTED" : "SHEARS_EFFECTIVE") : "SHEARS_NOT_EFFECTIVE")
                    + "#saveDecision=not_applicable#reason=SHEARS"
                    + "#speed=not_evaluated");
            emittedCandidates++;
        }

        public void logReturn(Slot bestToolSlot, String decisionReason, double highestSpeed) {
            if (!enabled) {
                return;
            }
            String fingerprint = fingerprint(bestToolSlot, decisionReason, highestSpeed);
            if (fingerprint.equals(lastBestToolSlotFingerprint)) {
                return;
            }
            lastBestToolSlotFingerprint = fingerprint;

            ChatClefDiagnostics.logBoundary("BEST_TOOL_SLOT_DECISION", "storage_helper_get_best_tool_slot", null,
                    "decisionReason", decisionReason,
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> targetState),
                    "targetBlockId", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.getBlock()),
                    "targetRequiresTool", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.isToolRequired()),
                    "minimumMiningRequirement", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : MiningRequirement.getMinimumRequirementForBlock(targetState.getBlock())),
                    "selectedSlot", ChatClefDiagnostics.slotSummary(bestToolSlot),
                    "selectedStack", stackDetails(slotStack(bestToolSlot)),
                    "selectedSpeed", speedValue(highestSpeed),
                    "candidateCount", candidateCount,
                    "toolCandidateCount", toolCandidateCount,
                    "defaultSuitableToolCount", defaultSuitableToolCount,
                    "savedSuitableToolCount", savedSuitableToolCount,
                    "eligibleToolCount", eligibleToolCount,
                    "shearsCandidateCount", shearsCandidateCount,
                    "effectiveShearsCount", effectiveShearsCount,
                    "truncatedCandidateCount", Math.max(0, candidateCount - emittedCandidates),
                    "candidateSummary", candidates.toString());
        }

        private String fingerprint(Slot bestToolSlot, String decisionReason, double highestSpeed) {
            return decisionReason
                    + "|" + ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.getBlock())
                    + "|" + ChatClefDiagnostics.slotSummary(bestToolSlot)
                    + "|" + stackFingerprint(slotStack(bestToolSlot))
                    + "|" + speedValue(highestSpeed)
                    + "|" + candidateCount
                    + "|" + toolCandidateCount
                    + "|" + defaultSuitableToolCount
                    + "|" + savedSuitableToolCount
                    + "|" + eligibleToolCount
                    + "|" + shearsCandidateCount
                    + "|" + effectiveShearsCount
                    + "|" + candidates;
        }
    }

    private static String toolOutcome(boolean defaultSuitable, boolean shouldSave, boolean becameBest) {
        if (!defaultSuitable) {
            return "SKIP_NOT_SUITABLE";
        }
        if (shouldSave) {
            return "SKIP_SHOULD_SAVE";
        }
        return becameBest ? "ELIGIBLE_NEW_BEST" : "ELIGIBLE_NOT_BEST";
    }

    private static String saveDecision(AltoClef mod, ItemStack stack, BlockState targetState, boolean observedShouldSave) {
        return ChatClefDiagnostics.safeValue(() -> {
            if (stack == null) {
                return "result=unavailable#reason=NO_STACK";
            }
            if (targetState == null) {
                return "result=unavailable#reason=NO_TARGET_STATE";
            }

            Item item = stack.getItem();
            if (item != Items.IRON_PICKAXE) {
                return "result=false#reason=NOT_IRON_PICKAXE";
            }

            boolean hasDiamondPickaxe = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE);
            if (hasDiamondPickaxe) {
                return "result=false#reason=HAS_DIAMOND_PICKAXE";
            }

            Block block = targetState.getBlock();
            boolean diamondRelatedBlock = block.equals(Blocks.DIAMOND_BLOCK)
                    || block.equals(Blocks.DIAMOND_ORE)
                    || block.equals(Blocks.DEEPSLATE_DIAMOND_ORE);
            int damage = stack.getDamage();
            int maxDamage = stack.getMaxDamage();
            boolean criticalDurability = damage + 8 > maxDamage;
            boolean lowDurability = damage + 30 > maxDamage;
            MiningRequirement minimumRequirement = MiningRequirement.getMinimumRequirementForBlock(block);

            String reason = "NOT_LOW_DURABILITY";
            if (criticalDurability) {
                reason = diamondRelatedBlock ? "CRITICAL_DURABILITY_DIAMOND_RELATED" : "CRITICAL_DURABILITY_NON_DIAMOND";
            } else if (lowDurability) {
                reason = minimumRequirement.equals(MiningRequirement.IRON)
                        ? "LOW_DURABILITY_IRON_REQUIRED"
                        : "LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED";
            }

            return "result=" + observedShouldSave
                    + "#reason=" + reason
                    + "#hasDiamondPickaxe=" + hasDiamondPickaxe
                    + "#damage=" + damage
                    + "#maxDamage=" + maxDamage
                    + "#damagePlus8=" + (damage + 8)
                    + "#damagePlus30=" + (damage + 30)
                    + "#diamondRelatedBlock=" + diamondRelatedBlock
                    + "#minimumMiningRequirement=" + minimumRequirement;
        });
    }

    private static String stackDetails(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.itemStackSummary(stack)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#isTool=" + ChatClefDiagnostics.safeValue(() -> stack.getItem() instanceof ToolItem)
                + "#isShears=" + ChatClefDiagnostics.safeValue(() -> stack.getItem() == Items.SHEARS)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString);
    }

    private static ItemStack slotStack(Slot slot) {
        if (slot == null) {
            return null;
        }
        try {
            return StorageHelper.getItemStackInSlot(slot);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static String stackFingerprint(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.safeValue(() -> stack.getItem())
                + "#count=" + ChatClefDiagnostics.safeValue(stack::getCount)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#empty=" + ChatClefDiagnostics.safeValue(stack::isEmpty);
    }

    private static String speedValue(double speed) {
        if (Double.isNaN(speed)) {
            return "not_evaluated";
        }
        if (speed == Double.NEGATIVE_INFINITY) {
            return "none";
        }
        return Double.toString(speed);
    }
}
