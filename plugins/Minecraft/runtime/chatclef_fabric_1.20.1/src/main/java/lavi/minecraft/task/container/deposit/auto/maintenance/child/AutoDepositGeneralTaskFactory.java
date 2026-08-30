package lavi.minecraft.task.container.deposit.auto.maintenance.child;

import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPlacementTaskOwner;
import lavi.minecraft.task.container.deposit.handoff.DepositAllPostPlaceHandoff;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

//20260831_kpopmodder: Construct automatic general-deposit children with isolated handoff state.
/** Creates automatic general-deposit children with operation-local handoff state. */
public final class AutoDepositGeneralTaskFactory {
    private final Supplier<DepositAllPlacementTaskOwner> placementOwnerFactory;
    private final Supplier<DepositAllPostPlaceHandoff> handoffFactory;

    public AutoDepositGeneralTaskFactory() {
        this(DepositAllPlacementTaskOwner::retaining, DepositAllPostPlaceHandoff::singleTick);
    }

    AutoDepositGeneralTaskFactory(
            Supplier<DepositAllPlacementTaskOwner> placementOwnerFactory,
            Supplier<DepositAllPostPlaceHandoff> handoffFactory) {
        this.placementOwnerFactory = Objects.requireNonNull(
                placementOwnerFactory,
                "placementOwnerFactory"
        );
        this.handoffFactory = Objects.requireNonNull(handoffFactory, "handoffFactory");
    }

    public List<DepositAllTask> create(ItemTarget[] targets) {
        List<DepositAllTask> result = new ArrayList<>();
        for (ItemTarget target : targets) {
            result.add(new DepositAllTask(
                    false,
                    Objects.requireNonNull(placementOwnerFactory.get(), "placement owner"),
                    Objects.requireNonNull(handoffFactory.get(), "post-place handoff"),
                    target
            ));
        }
        return List.copyOf(result);
    }
}
