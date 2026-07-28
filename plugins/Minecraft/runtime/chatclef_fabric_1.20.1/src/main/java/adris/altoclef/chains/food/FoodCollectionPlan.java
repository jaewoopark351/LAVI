package adris.altoclef.chains.food;

import adris.altoclef.tasksystem.Task;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;

//20260728_kpopmodder: Keeps the selected food action data separate from selector control flow.
public final class FoodCollectionPlan {
    private final Task task;
    private final Entity huntEntity;
    private final Item huntRawFood;
    private final String debugState;
    private final String stateKey;
    private final String detail;

    private FoodCollectionPlan(Task task, Entity huntEntity, Item huntRawFood, String debugState, String stateKey, String detail) {
        this.task = task;
        this.huntEntity = huntEntity;
        this.huntRawFood = huntRawFood;
        this.debugState = debugState;
        this.stateKey = stateKey;
        this.detail = detail;
    }

    static FoodCollectionPlan resource(Task task, String debugState, String stateKey, String detail) {
        return new FoodCollectionPlan(task, null, null, debugState, stateKey, detail);
    }

    static FoodCollectionPlan hunt(Task task, Entity entity, Item rawFood, String debugState, String stateKey, String detail) {
        return new FoodCollectionPlan(task, entity, rawFood, debugState, stateKey, detail);
    }

    public Task getTask() {
        return task;
    }

    public boolean isHunt() {
        return huntEntity != null;
    }

    public Entity getHuntEntity() {
        return huntEntity;
    }

    public Item getHuntRawFood() {
        return huntRawFood;
    }

    public String getDebugState() {
        return debugState;
    }

    public String getStateKey() {
        return stateKey;
    }

    public String getDetail() {
        return detail;
    }
}
