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
import lavi.minecraft.task.container.deposit.DepositAllStoreTaskGeneration;
import lavi.minecraft.task.container.deposit.DepositAllContainerTargetState;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;
import net.minecraft.block.Block;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Objects;
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
    private final DepositAllStoreTaskGeneration _storeTaskGeneration;
    private final DepositAllPlacementTaskOwner _placementTaskOwner;
    private final DepositAllPostPlaceHandoff _postPlaceHandoff;
    private final MovementProgressChecker _progressChecker = new MovementProgressChecker();
    private final ContainerStoredTracker _storedItems = new ContainerStoredTracker(slot -> true);

    public DepositAllTask(boolean getIfNotPresent, ItemTarget... toStore) {
        this(
                getIfNotPresent,
                new DepositAllStoreTaskGeneration(),
                DepositAllPlacementTaskOwner.ephemeral(),
                DepositAllPostPlaceHandoff.disabled(),
                toStore
        );
    }

    public DepositAllTask(
            boolean getIfNotPresent,
            DepositAllPlacementTaskOwner placementTaskOwner,
            DepositAllPostPlaceHandoff postPlaceHandoff,
            ItemTarget... toStore) {
        this(
                getIfNotPresent,
                new DepositAllStoreTaskGeneration(),
                placementTaskOwner,
                postPlaceHandoff,
                toStore
        );
    }

    DepositAllTask(boolean getIfNotPresent,
                   DepositAllStoreTaskGeneration storeTaskGeneration,
                   ItemTarget... toStore) {
        this(
                getIfNotPresent,
                storeTaskGeneration,
                DepositAllPlacementTaskOwner.ephemeral(),
                DepositAllPostPlaceHandoff.disabled(),
                toStore
        );
    }

    private DepositAllTask(
            boolean getIfNotPresent,
            DepositAllStoreTaskGeneration storeTaskGeneration,
            DepositAllPlacementTaskOwner placementTaskOwner,
            DepositAllPostPlaceHandoff postPlaceHandoff,
            ItemTarget... toStore) {
        _getIfNotPresent = getIfNotPresent;
        _toStore = toStore;
        _storeTaskGeneration = Objects.requireNonNull(storeTaskGeneration, "storeTaskGeneration");
        _placementTaskOwner = Objects.requireNonNull(placementTaskOwner, "placementTaskOwner");
        _postPlaceHandoff = Objects.requireNonNull(postPlaceHandoff, "postPlaceHandoff");
        if (_placementTaskOwner.retainingIdentity() != _postPlaceHandoff.enabled()) {
            throw new IllegalArgumentException(
                    "post-place handoff requires retaining placement ownership"
            );
        }
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
                _storeTaskGeneration.clear();
                _progressChecker.reset();
            }
            StoreDepositDiagnostics.observeAutomaticMovementResult(
                    this,
                    selectedTarget,
                    targetInvalidationReason,
                    progressCheckEvaluated,
                    progressCheckOk,
                    !"NONE".equals(targetInvalidationReason)
            );
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
        //20260730_kpopmodder: Minimal LAVI divergence at the verified ChatClef engine boundary.
        if (deferAfterCompletedPlacement()) {
            setDebugState("Completing placed-container child handoff");
            return null;
        }
        if (selectedTarget.isPresent()) {
            BlockPos fixedTarget = selectedTarget.get();
            boolean refreshedFinishedGeneration = _storeTaskGeneration.clearIfFinishedWithRemainingWork(
                    fixedTarget,
                    notStored
            );
            int storeGenerationBefore = _storeTaskGeneration.generationId();
            Task storeTask = storeTaskForSelectedTarget(fixedTarget, notStored);
            boolean createdStoreGeneration = _storeTaskGeneration.generationId() != storeGenerationBefore;
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
                    "progressFailureWillRequestUnreachable", progressCheckEvaluated && !progressCheckOk,
                    "storeGenerationId", _storeTaskGeneration.generationId(),
                    "storeGenerationCreated", createdStoreGeneration,
                    "storeGenerationRefreshReason", refreshedFinishedGeneration ? "CHILD_FINISHED_REMAINING_WORK" : "NONE");
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
                    "childTaskClass", StoreInContainerTask.class.getName(),
                     "storeGenerationId", _storeTaskGeneration.generationId(),
                     "storeGenerationCreated", createdStoreGeneration,
                     "storeGenerationRefreshReason", refreshedFinishedGeneration ? "CHILD_FINISHED_REMAINING_WORK" : "NONE");
            StoreDepositDiagnostics.stageAutomaticRouteCandidate(
                    this,
                    storeTask,
                    fixedTarget,
                    _storeTaskGeneration.generationId(),
                    selectedNewTarget
            );
            return storeTask;
        }

        _progressChecker.reset();
        _storeTaskGeneration.clear();
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
                return _placementTaskOwner.getOrCreate(couldPlace, () ->
                        new PlaceBlockNearbyTask(canPlace -> {
                            // For chests, above must be air OR breakable.
                            if (WorldHelper.isChest(couldPlace)) {
                                return WorldHelper.isAir(canPlace.up()) || WorldHelper.canBreak(canPlace.up());
                            }
                            return true;
                        }, couldPlace));
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

    private boolean deferAfterCompletedPlacement() {
        PlaceBlockNearbyTask placementTask = _placementTaskOwner.currentTask();
        boolean placementActive = placementTask != null && placementTask.isActive();
        boolean placementFinished = placementActive && placementTask.isFinished();
        if (!_postPlaceHandoff.shouldDefer(
                placementTask,
                placementActive,
                placementFinished
        )) {
            return false;
        }
        _placementTaskOwner.clear(placementTask);
        return true;
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

    Task storeTaskForSelectedTarget(BlockPos fixedTarget, ItemTarget[] notStored) {
        return _storeTaskGeneration.getOrCreate(fixedTarget, _getIfNotPresent, notStored);
    }

    @Override
    protected String toDebugString() {
        return "Storing in any container: " + Arrays.toString(_toStore);
    }
}
