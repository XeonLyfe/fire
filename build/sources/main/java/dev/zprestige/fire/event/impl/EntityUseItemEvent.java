package dev.zprestige.fire.event.impl;

import dev.zprestige.fire.event.bus.Event;
import dev.zprestige.fire.event.bus.Stage;
import net.minecraft.entity.Entity;

public class EntityUseItemEvent extends Event {
    protected final Entity entity;
    public EntityUseItemEvent(final Entity entity) {
        super(Stage.None, false);
        this.entity = entity;
    }

    public Entity getEntity(){
        return entity;
    }
}