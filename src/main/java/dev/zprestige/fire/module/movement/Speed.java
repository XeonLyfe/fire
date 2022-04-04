package dev.zprestige.fire.module.movement;

import com.mojang.realmsclient.gui.ChatFormatting;
import dev.zprestige.fire.Main;
import dev.zprestige.fire.events.eventbus.annotation.RegisterListener;
import dev.zprestige.fire.events.impl.KeyEvent;
import dev.zprestige.fire.events.impl.MoveEvent;
import dev.zprestige.fire.events.impl.TickEvent;
import dev.zprestige.fire.module.Module;
import dev.zprestige.fire.settings.impl.ComboBox;
import dev.zprestige.fire.settings.impl.Key;
import dev.zprestige.fire.settings.impl.Slider;
import dev.zprestige.fire.settings.impl.Switch;
import dev.zprestige.fire.util.impl.EntityUtil;
import net.minecraft.init.MobEffects;
import net.minecraft.util.MovementInput;
import org.lwjgl.input.Keyboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Speed extends Module {
    public final ComboBox speedMode = Menu.ComboBox("Speed Mode", "Strafe", new String[]{"OnGround", "Strafe"});
    public final ComboBox dataMode = Menu.ComboBox("Data Mode", "Mode", new String[]{"Mode", "Factor"});
    public final Key switchKey = Menu.Key("Switch Key", Keyboard.KEY_NONE);
    public final Switch strict = Menu.Switch("Strict", false);
    public final Switch liquids = Menu.Switch("Liquids", false);
    public final Switch useTimer = Menu.Switch("Use Timer", false);
    public final Slider timerAmount = Menu.Slider("Timer Amount", 1.0f, 0.9f, 2.0f).visibility(z -> useTimer.GetSwitch());
    public final Switch velocityBoost = Menu.Switch("Velocity Boost", false);
    public final Slider boostAmplifier = Menu.Slider("Velocity Boost Amplifier", 10.0f, 1.0f, 20.0f).visibility(z -> velocityBoost.GetSwitch());
    public final Slider strafeFactor = Menu.Slider("Strafe Factor", 1.0f, 0.1f, 3.0f).visibility(z -> !strict.GetSwitch());
    protected double previousDistance, motionSpeed;
    protected float lastHealth;
    protected int currentState = 1;
    protected HashMap<Long, Float> damageMap = new HashMap<>();

    @Override
    public void onDisable() {
        Main.tickManager.syncTimer();
    }

    @RegisterListener
    public void onKeyEvent(final KeyEvent event) {
        if (switchKey.GetKey() == event.getKey()) {
            if (speedMode.GetCombo().equals("Strafe")) {
                speedMode.setValue("OnGround");
            } else {
                speedMode.setValue("Strafe");
            }
            sendSwitchMessage();
        }
    }

    protected void sendSwitchMessage() {
        Main.notificationManager.addNotifications("Speed mode switched to " + speedMode.GetCombo() + ".");
        Main.chatManager.sendRemovableMessage(ChatFormatting.WHITE + "" + ChatFormatting.BOLD + "Speed" + ChatFormatting.RESET + " mode switched to " + Main.chatManager.prefixColor + speedMode.GetCombo() + ChatFormatting.RESET + ".", 1);
    }

    @RegisterListener
    public void onTick(final TickEvent event) {
        previousDistance = Math.sqrt((mc.player.posX - mc.player.prevPosX) * (mc.player.posX - mc.player.prevPosX) + (mc.player.posZ - mc.player.prevPosZ) * (mc.player.posZ - mc.player.prevPosZ));
        if (useTimer.GetSwitch()) {
            Main.tickManager.setTimer(timerAmount.GetSlider());
        }
        final float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float damage = 0.0f;
        for (Map.Entry<Long, Float> entry : new HashMap<>(damageMap).entrySet()) {
            double val = entry.getValue() / (((System.currentTimeMillis() - entry.getKey())) / boostAmplifier.GetSlider());
            if (val < 0.1) {
                damageMap.remove(entry.getKey());
            }
            damage += val;
        }
        final float dmg = lastHealth - health;
        if (dmg > 0) {
            damageMap.put(System.currentTimeMillis(), dmg);
        }
        lastHealth = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (strict.GetSwitch()) {
            strafeFactor.setValue(0.9f);
        } else {
            if (velocityBoost.GetSwitch()) {
                strafeFactor.setValue(Math.max(1.0f, damage));
            }
        }
    }

    @Override
    public String getData() {
        switch (dataMode.GetCombo()) {
            case "Factor":
                return strafeFactor.GetSlider() + "";
            case "Mode":
                return speedMode.GetCombo();
        }
        return "";
    }

    @RegisterListener
    public void onMove(final MoveEvent event) {
        if (!nullCheck() || (!liquids.GetSwitch() && (mc.player.isInWater() || mc.player.isInLava() || mc.player.isSpectator())) || mc.player.isElytraFlying()) {
            return;
        }
        if (!mc.player.isSprinting()) {
            mc.player.setSprinting(true);
        }
        switch (speedMode.GetCombo()) {
            case "Strafe":
                switch (currentState) {
                    case 0:
                        ++currentState;
                        previousDistance = 0.0;
                        break;
                    case 1:
                    default:
                        if ((mc.world.getCollisionBoxes(mc.player, mc.player.getEntityBoundingBox().offset(0.0, mc.player.motionY, 0.0)).size() > 0 || mc.player.collidedVertically) && currentState > 0) {
                            currentState = mc.player.moveForward == 0.0F && mc.player.moveStrafing == 0.0F ? 0 : 1;
                        }
                        motionSpeed = previousDistance - previousDistance / 159.0;
                        break;
                    case 2:
                        double var2 = strict.GetSwitch() ? 0.42 : 0.40123128;
                        if ((mc.player.moveForward != 0.0f || mc.player.moveStrafing != 0.0f) && mc.player.onGround) {
                            if (mc.player.isPotionActive(MobEffects.JUMP_BOOST)) {
                                var2 += (Objects.requireNonNull(mc.player.getActivePotionEffect(MobEffects.JUMP_BOOST)).getAmplifier() + 1) * 0.1f;
                            }
                            event.setMotionY(mc.player.motionY = var2);
                            motionSpeed *= 2.149;
                        }
                        break;
                    case 3:
                        motionSpeed = previousDistance - 0.76 * (previousDistance - EntityUtil.getBaseMotionSpeed() * strafeFactor.GetSlider());
                }
                motionSpeed = Math.max(motionSpeed, EntityUtil.getBaseMotionSpeed() * strafeFactor.GetSlider());
                double moveForward = mc.player.movementInput.moveForward;
                double moveStrafe = mc.player.movementInput.moveStrafe;
                double yaw = mc.player.rotationYaw;
                if (moveForward != 0.0 && moveStrafe != 0.0) {
                    moveForward *= Math.sin(0.7853981633974483);
                    moveStrafe *= Math.cos(0.7853981633974483);
                }
                final double sin = Math.sin(Math.toRadians(yaw));
                final double cos = Math.cos(Math.toRadians(yaw));
                event.setMotionX((moveForward * motionSpeed * -sin) + (moveStrafe * motionSpeed * cos) * 0.99);
                event.setMotionZ((moveForward * motionSpeed * cos) - (moveStrafe * motionSpeed * -sin) * 0.99);
                ++currentState;
                break;
            case "OnGround":
                if (!(mc.player.isSneaking() || mc.player.movementInput.moveForward == 0.0f && mc.player.movementInput.moveStrafe == 0.0f) || !mc.player.onGround) {
                    MovementInput movementInput = mc.player.movementInput;
                    float moveForward1 = movementInput.moveForward;
                    float moveStrafe1 = movementInput.moveStrafe;
                    float rotationYaw = mc.player.rotationYaw;
                    if (moveForward1 == 0.0 && moveStrafe1 == 0.0) {
                        event.setMotionX(0.0);
                        event.setMotionZ(0.0);
                    } else {
                        if (moveForward1 != 0.0) {
                            if (moveStrafe1 > 0.0) {
                                rotationYaw += (moveForward1 > 0.0 ? -45 : 45);
                            } else if (moveStrafe1 < 0.0) {
                                rotationYaw += (moveForward1 > 0.0 ? 45 : -45);
                            }
                            moveStrafe1 = 0.0f;
                        }
                        moveStrafe1 = moveStrafe1 == 0.0f ? moveStrafe1 : (moveStrafe1 > 0.0 ? 1.0f : -1.0f);
                        final double cos1 = Math.cos(Math.toRadians(rotationYaw + 90.0f));
                        final double sin1 = Math.sin(Math.toRadians(rotationYaw + 90.0f));
                        event.setMotionX(moveForward1 * EntityUtil.getMaxSpeed() * cos1 + moveStrafe1 * EntityUtil.getMaxSpeed() * sin1);
                        event.setMotionZ(moveForward1 * EntityUtil.getMaxSpeed() * sin1 - moveStrafe1 * EntityUtil.getMaxSpeed() * cos1);
                    }
                }
                break;
        }
    }
}
