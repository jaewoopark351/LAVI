package adris.altoclef.tasks.container;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.TaskCatalogue;
import adris.altoclef.tasks.construction.PlaceBlockNearbyTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.progresscheck.MovementProgressChecker;
import lavi.minecraft.diagnostics.container.store.StoreInAnyContainerDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.task.container.deposit.DepositAllContainerEligibility;
import lavi.minecraft.task.container.deposit.DepositAllContainerSelector;
import lavi.minecraft.task.container.deposit.DepositAllContainerTargetState;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Optional;

//20260826_kpopmodder: Added an independent deposit_all task as a behavior-preserving copy of StoreInAnyContainerTask.
/**
 * Dumps items in any container, placing a chest if we can't find any.
 */
public class DepositAllTask extends Task {

    private static final int TOO_FAR_RANGE = 50;
    private static final int TOO_FAR_RANGE_EXTRA = 70;

    private final ItemTarget[] _toStore;
    private final boolean _getIfNotPresent;
    private final DepositAllContainerEligibility _containerEligibility = new DepositAllContainerEligibility();
    private final DepositAllContainerSelector _containerSelector = new DepositAllContainerSelector();
    private final DepositAllContainerTargetState _targetState = new DepositAllContainerTargetState();
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final ContainerStoredTracker _storedItems = new ContainerStoredTracker(slot -> true);

    public DepositAllTask(boolean getIfNotPresent, ItemTarget... toStore) {
        _getIfNotPresent = getIfNotPresent;
        _toStore = toStore;
    }

    @Override
    protected void onStart() {
        StoreInAnyContainerDiagnostics.logStart(this, _getIfNotPresent, _toStore);
        StoreDepositDiagnostics.bindRootTracker(this, _storedItems);
        _storedItems.startTracking();
        _containerEligibility.reset();
        _targetState.clear();
        _progressChecker.reset();
    }

    @Override
    protected Task onTick() {
        AltoClef mod = AltoClef.getInstance();

        // Get more if we don't have & "get if not present" is true.
        if (_getIfNotPresent) {
            for (ItemTarget target : _toStore) {
                int inventoryNeed = target.getTargetCount() - _storedItems.getStoredCount(target.getMatches());
                int inventoryCount = mod.getItemStorage().getItemCount(target);
                if (inventoryNeed > inventoryCount) {
                    StoreInAnyContainerDiagnostics.logBranch("return_get_missing_item",
                            mod,
                            this,
                            _getIfNotPresent,
                            _toStore,
                            null,
                            _storedItems,
                            null,
                            false,
                            false,
                            _targetState.selectedTarget().orElse(null),
                            _containerEligibility.dungeonChestCount(),
                            _containerEligibility.nonDungeonChestCount(),
                            "missingTarget", target,
                            "inventoryNeed", inventoryNeed,
                            "inventoryCount", inventoryCount,
                            "childTaskClass", "TaskCatalogue.getItemTask");
                    return TaskCatalogue.getItemTask(new ItemTarget(target, inventoryNeed));
                }
            }
        }

        // ItemTargets we haven't stored yet
        ItemTarget[] notStored = _storedItems.getUnstoredItemTargetsYouCanStore(mod, _toStore);

        Optional<BlockPos> rawClosest = mod.getBlockScanner().getNearestBlock(StoreInContainerTask.CONTAINER_BLOCKS);
        boolean rawClosestWithinRange = rawClosest.isPresent()
                && rawClosest.get().isWithinDistance(mod.getPlayer().getPos(), TOO_FAR_RANGE);

        Optional<BlockPos> selectedBeforeDecision = _targetState.selectedTarget();
        String targetInvalidationReason = "NONE";
        boolean progressCheckEvaluated = false;
        boolean progressCheckOk = true;

        if (selectedBeforeDecision.isPresent()) {
            BlockPos selectedTarget = selectedBeforeDecision.get();
            boolean selectedWithinExtraRange = _targetState.isSelectedWithin(
                    mod.getPlayer().getPos(),
                    TOO_FAR_RANGE_EXTRA);
            if (!selectedWithinExtraRange) {
                targetInvalidationReason = "OUTSIDE_70";
            } else if (mod.getBlockScanner().isUnreachable(selectedTarget)) {
                targetInvalidationReason = "BLOCK_SCANNER_UNREACHABLE";
            } else if (mod.getChunkTracker().isChunkLoaded(selectedTarget)) {
                DepositAllContainerEligibility.Evaluation evaluation = _containerEligibility.evaluate(
                        mod,
                        selectedTarget,
                        StoreInContainerTask.CONTAINER_BLOCKS
                );
                if (!evaluation.accepted()) {
                    targetInvalidationReason = evaluation.outcome().name();
                }
            }

            if ("NONE".equals(targetInvalidationReason)) {
                progressCheckEvaluated = true;
                progressCheckOk = _progressChecker.check(mod);
                if (!progressCheckOk) {
                    Debug.logMessage("Failed to open container. Suggesting it may be unreachable.");
                    mod.getBlockScanner().requestBlockUnreachable(selectedTarget, 2);
                    targetInvalidationReason = "MOVEMENT_PROGRESS_FAILED";
                }
            }

            if (!"NONE".equals(targetInvalidationReason)) {
                _targetState.clear();
                _progressChecker.reset();
            }
        }

        Optional<BlockPos> filteredCandidate = Optional.empty();
        boolean filteredSearchPerformed = false;
        boolean filteredCandidateWithinRange = false;
        boolean selectedNewTarget = false;
        if (_targetState.selectedTarget().isEmpty()) {
            filteredSearchPerformed = true;
            StoreDepositDiagnostics.beginDepositAllParentFilteredSearchObservation(this, rawClosest.orElse(null));
            boolean scannerCallCompletedNormally = false;
            try {
                filteredCandidate = _containerSelector.select(
                        mod,
                        containerPos -> {
                            DepositAllContainerEligibility.Evaluation evaluation = _containerEligibility.evaluate(
                                    mod,
                                    containerPos,
                                    StoreInContainerTask.CONTAINER_BLOCKS
                            );
                            StoreDepositDiagnostics.observeDepositAllContainerEligibility(
                                    evaluation.position(),
                                    evaluation.outcome().name()
                            );
                            return evaluation.accepted();
                        },
                        StoreInContainerTask.CONTAINER_BLOCKS
                );
                scannerCallCompletedNormally = true;
            } finally {
                StoreDepositDiagnostics.endDepositAllParentFilteredSearchObservation(
                        this,
                        scannerCallCompletedNormally
                );
            }
            filteredCandidateWithinRange = filteredCandidate.isPresent()
                    && filteredCandidate.get().isWithinDistance(mod.getPlayer().getPos(), TOO_FAR_RANGE);
            if (filteredCandidateWithinRange) {
                selectedNewTarget = _targetState.selectIfWithin(
                        filteredCandidate.get(),
                        mod.getPlayer().getPos(),
                        TOO_FAR_RANGE
                );
                if (selectedNewTarget) {
                    _progressChecker.reset();
                }
            }
        }

        Optional<BlockPos> selectedTarget = _targetState.selectedTarget();
        boolean selectedWithinExtraRange = _targetState.isSelectedWithin(
                mod.getPlayer().getPos(),
                TOO_FAR_RANGE_EXTRA);
        if (selectedTarget.isPresent()) {
            BlockPos fixedTarget = selectedTarget.get();
            String candidateDecisionOutcome = selectedNewTarget
                    ? "FILTERED_TARGET_WITHIN_50"
                    : "SELECTED_TARGET_WITHIN_70";

            setDebugState("Going to container and depositing items");
            StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                    this,
                    "OPEN_EXISTING",
                    true,
                    rawClosest.orElse(null),
                    rawClosest.isPresent(),
                    rawClosestWithinRange,
                    true,
                    selectedWithinExtraRange,
                    fixedTarget,
                    notStored,
                    "candidateDecisionOutcome", candidateDecisionOutcome,
                    "rawCandidateBehaviorEffect", "NONE",
                    "filteredSearchPerformed", filteredSearchPerformed,
                    "filteredCandidate", filteredCandidate.orElse(null),
                    "filteredCandidateWithin50", filteredCandidateWithinRange,
                    "selectedTarget", fixedTarget,
                    "selectedTargetSource", selectedNewTarget ? "FILTERED_SCAN" : "RETAINED",
                    "targetInvalidationReason", targetInvalidationReason,
                    "progressCheckEvaluated", progressCheckEvaluated,
                    "progressCheckOk", progressCheckOk,
                    "progressFailureWillRequestUnreachable", progressCheckEvaluated && !progressCheckOk);
            if (filteredSearchPerformed) {
                StoreDepositDiagnostics.logFilteredSearchResult(
                        this,
                        filteredCandidate,
                        StoreInContainerTask.CONTAINER_BLOCKS
                );
            }
            StoreDepositDiagnostics.logPursuitDecision(
                    this,
                    selectedBeforeDecision.orElse(null),
                    fixedTarget,
                    selectedNewTarget ? "SELECT_FILTERED_TARGET" : "RETAIN_SELECTED_TARGET"
            );
            StoreInAnyContainerDiagnostics.logBranch("return_open_existing_container",
                    mod,
                    this,
                    _getIfNotPresent,
                    _toStore,
                    notStored,
                    _storedItems,
                    rawClosest.orElse(null),
                    rawClosestWithinRange,
                    selectedWithinExtraRange,
                    fixedTarget,
                    _containerEligibility.dungeonChestCount(),
                    _containerEligibility.nonDungeonChestCount(),
                    "filteredSearchPerformed", filteredSearchPerformed,
                    "filteredCandidate", filteredCandidate.orElse(null),
                    "filteredCandidateWithin50", filteredCandidateWithinRange,
                    "selectedTarget", fixedTarget,
                    "targetInvalidationReason", targetInvalidationReason,
                    "progressCheckEvaluated", progressCheckEvaluated,
                    "progressCheckOk", progressCheckOk,
                    "childTaskClass", StoreInContainerTask.class.getName());
            return new StoreInContainerTask(fixedTarget, _getIfNotPresent, notStored);
        }

        _progressChecker.reset();
        String fallbackDecisionOutcome = filteredCandidate.isEmpty()
                ? "NO_FILTERED_TARGET"
                : "FILTERED_TARGET_OUTSIDE_50";
        // Craft + place chest nearby
        for (Block couldPlace : StoreInContainerTask.CONTAINER_BLOCKS) {
            boolean hasContainerBlockItem = mod.getItemStorage().hasItem(couldPlace.asItem());
            if (hasContainerBlockItem) {
                StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                        this,
                        "PLACE_CONTAINER_NEARBY",
                        true,
                        rawClosest.orElse(null),
                        rawClosest.isPresent(),
                        rawClosestWithinRange,
                        false,
                        false,
                        null,
                        notStored,
                        "candidateDecisionOutcome", fallbackDecisionOutcome,
                        "rawCandidateBehaviorEffect", "NONE",
                        "filteredSearchPerformed", filteredSearchPerformed,
                        "filteredCandidate", filteredCandidate.orElse(null),
                        "filteredCandidateWithin50", filteredCandidateWithinRange,
                        "selectedTarget", null,
                        "targetInvalidationReason", targetInvalidationReason,
                        "containerBlockItem", couldPlace.asItem(),
                        "fallbackContainerItemPresent", true);
                if (filteredSearchPerformed) {
                    StoreDepositDiagnostics.logFilteredSearchResult(
                            this,
                            filteredCandidate,
                            StoreInContainerTask.CONTAINER_BLOCKS
                    );
                }
                StoreInAnyContainerDiagnostics.logBranch("return_place_container_nearby",
                        mod,
                        this,
                        _getIfNotPresent,
                        _toStore,
                        notStored,
                        _storedItems,
                        rawClosest.orElse(null),
                        rawClosestWithinRange,
                        false,
                        null,
                        _containerEligibility.dungeonChestCount(),
                        _containerEligibility.nonDungeonChestCount(),
                        "filteredSearchPerformed", filteredSearchPerformed,
                        "filteredCandidate", filteredCandidate.orElse(null),
                        "filteredCandidateWithin50", filteredCandidateWithinRange,
                        "targetInvalidationReason", targetInvalidationReason,
                        "containerBlockItem", couldPlace.asItem(),
                        "childTaskClass", PlaceBlockNearbyTask.class.getName());
                setDebugState("Placing container nearby");
                return new PlaceBlockNearbyTask(canPlace -> {
                    // For chests, above must be air OR breakable.
                    if (WorldHelper.isChest(couldPlace)) {
                        return WorldHelper.isAir(canPlace.up()) || WorldHelper.canBreak(canPlace.up());
                    }
                    return true;
                }, couldPlace);
            }
        }
        setDebugState("Obtaining a chest item (by default)");
        StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                this,
                "OBTAIN_CHEST",
                true,
                rawClosest.orElse(null),
                rawClosest.isPresent(),
                rawClosestWithinRange,
                false,
                false,
                null,
                notStored,
                "candidateDecisionOutcome", fallbackDecisionOutcome,
                "rawCandidateBehaviorEffect", "NONE",
                "filteredSearchPerformed", filteredSearchPerformed,
                "filteredCandidate", filteredCandidate.orElse(null),
                "filteredCandidateWithin50", filteredCandidateWithinRange,
                "selectedTarget", null,
                "targetInvalidationReason", targetInvalidationReason,
                "requestedItem", Items.CHEST,
                "requestedCount", 1,
                "fallbackContainerItemPresent", false);
        if (filteredSearchPerformed) {
            StoreDepositDiagnostics.logFilteredSearchResult(
                    this,
                    filteredCandidate,
                    StoreInContainerTask.CONTAINER_BLOCKS
            );
        }
        StoreInAnyContainerDiagnostics.logBranch("return_obtain_chest_item",
                mod,
                this,
                _getIfNotPresent,
                _toStore,
                notStored,
                _storedItems,
                rawClosest.orElse(null),
                rawClosestWithinRange,
                false,
                null,
                _containerEligibility.dungeonChestCount(),
                _containerEligibility.nonDungeonChestCount(),
                "filteredSearchPerformed", filteredSearchPerformed,
                "filteredCandidate", filteredCandidate.orElse(null),
                "filteredCandidateWithin50", filteredCandidateWithinRange,
                "targetInvalidationReason", targetInvalidationReason,
                "requestedItem", Items.CHEST,
                "requestedCount", 1,
                "childTaskClass", "TaskCatalogue.getItemTask");
        return TaskCatalogue.getItemTask(Items.CHEST, 1);
    }

    @Override
    public boolean isFinished() {
        // We've stored all items
        return _storedItems.getUnstoredItemTargetsYouCanStore(AltoClef.getInstance(), _toStore).length == 0;
    }

    @Override
    protected void onStop(Task interruptTask) {
        StoreInAnyContainerDiagnostics.logStop(this, interruptTask, _getIfNotPresent, _toStore);
        _storedItems.stopTracking();
        _targetState.clear();
        _containerEligibility.reset();
        _progressChecker.reset();
    }

    @Override
    protected boolean isEqual(Task other) {
        if (other instanceof DepositAllTask task) {
            return task._getIfNotPresent == _getIfNotPresent && Arrays.equals(task._toStore, _toStore);
        }
        return false;
    }

    @Override
    protected String toDebugString() {
        return "Storing in any container: " + Arrays.toString(_toStore);
    }
}
