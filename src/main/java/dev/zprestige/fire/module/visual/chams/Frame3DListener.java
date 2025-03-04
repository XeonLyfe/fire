package dev.zprestige.fire.module.visual.chams;

import dev.zprestige.fire.event.bus.EventListener;
import dev.zprestige.fire.event.impl.FrameEvent;
import dev.zprestige.fire.util.impl.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.*;
import java.util.ArrayList;

public class Frame3DListener extends EventListener<FrameEvent.FrameEvent3D, Chams> {

    public Frame3DListener(final Chams chams) {
        super(FrameEvent.FrameEvent3D.class, chams);
    }

    @Override
    public void invoke(final Object object) {
        final FrameEvent.FrameEvent3D event = (FrameEvent.FrameEvent3D) object;
        final float partialTicks = event.getPartialTicks();
        final int fps = Minecraft.getDebugFPS();
        for (final Chams.PopEntity popEntity : new ArrayList<>(module.popEntities)) {
            ((EntityPlayer) popEntity.getEntity()).hurtTime = 0;
            final int alpha = (int) popEntity.getAlpha();
            final Entity entity = popEntity.getEntity();
            if (module.popAnimateVertical.GetSwitch()) {
                entity.posY += module.popVerticalAnimationSpeed.GetSlider() / 100.0f;
            }
            if (module.popFill.GetSwitch()) {
                final Color color = module.popFillColor.GetColor();
                module.prepareFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
                RenderUtil.renderEntity(entity, partialTicks);
                module.releaseFill();
            }
            if (module.popOutline.GetSwitch()) {
                final Color color = module.popOutlineColor.GetColor();
                module.prepareOutline(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha), module.popOutlineWidth.GetSlider());
                RenderUtil.renderEntity(entity, partialTicks);
                module.releaseOutline();
            }
            popEntity.updateAlpha(fps);
            if (popEntity.getAlpha() < 30) {
                module.popEntities.remove(popEntity);
            }
        }
    }
}
