package com.holybuckets.combat.client;

import com.holybuckets.combat.core.*;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import com.holybuckets.foundation.event.custom.ClientTickEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientDashManager implements IDashManager {

    private static final double ANTICIPATION_DRAG = 0.5D;
    private static final double RECOVERY_CARRY = 0.2D;
    private static final double LIFT_HEIGHT = 1.25D;
    private static final double GRAVITY = 0.08D;
    private static final double AIR_DRAG = 0.98D;
    private static final double LIFT_STEP = 0.01D;

    private static final Map<Integer, DashContext> CONTEXTS = new HashMap<>();
    private static int nextSeq = 0;

    public static void init(ClientEventRegistrar reg)
    {
        reg.registerOnClientTick(TickType.ON_SINGLE_TICK, ClientDashManager::onClientTick);
        reg.registerOnSimpleMessage(DashManager.MSG_START, ClientDashManager::add3rdPartyDashStart);
        reg.registerOnSimpleMessage(DashManager.MSG_RESULT, ClientDashManager::onResultMessage);
        reg.registerOnDisconnectedFromServer(event -> CONTEXTS.clear());
    }

    public static DashContext getActive(Entity entity) {
        if (entity == null) return null;
        DashContext ctx = CONTEXTS.get(entity.getId());
        return (ctx != null && ctx.isActive()) ? ctx : null;
    }

    public void tryDash(Player player, int invSlot)
    {
        if (!DashManager.canDash(player)) return;
        ItemStack stack = player.getInventory().getItem(invSlot);
        if( player.getCooldowns().isOnCooldown(stack.getItem()) ) return;

        LocalPlayer local = (LocalPlayer) player;
        DashDirection direction = DashDirection.fromInput(local.input.up,
         local.input.down,
         local.input.left,
         local.input.right);

        if (direction == null) return;
        DashContext ctx = CONTEXTS.computeIfAbsent(local.getId(), DashContext::new);
        int seq = ++nextSeq;
        if (!ctx.begin(direction, local.getYRot(), seq, DashManager.GLIDE_SPEED)) return;
        ctx.lift = isJumping(local);
        ctx.startY = local.getY();

        local.getCooldowns().addCooldown(stack.getItem(), DashManager.COOLDOWN_TICKS);
        local.level().playLocalSound(local.getX(), local.getY(), local.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS, DashManager.SOUND_VOLUME, DashManager.SOUND_PITCH, false);
        SimpleStringMessage.createAndFire(local, DashManager.MSG_REQUEST, direction.ordinal() + "," + seq + "," + invSlot);
    }

    public static void suppressInput(LocalPlayer player) {
        DashContext ctx = getActive(player);
        if (ctx == null || !ctx.isMoving()) return;
        player.xxa = 0.0F;
        player.zza = 0.0F;
        player.setJumping(false);
    }

    private static void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused() || CONTEXTS.isEmpty()) return;

        Iterator<Map.Entry<Integer, DashContext>> it = CONTEXTS.entrySet().iterator();
        while (it.hasNext()) {
            DashContext ctx = it.next().getValue();
            Entity entity = mc.level.getEntity(ctx.entityId);
            if (!(entity instanceof Player player) || entity.isRemoved()) {
                it.remove();
                continue;
            }

            if (player==mc.player)
                tickLocal(player, ctx);
            else ctx.tick();

            //if (ctx.phase == DashPhase.GLIDE) ClientDashRenderer.spawnGlideParticles(player, ctx);
            if (!ctx.isActive()) it.remove();
        }
    }

    private static void tickLocal(Player player, DashContext ctx) {
        if (!DashManager.canDash(player)) ctx.fire(DashEvent.INTERRUPTED);
        else if (ctx.phase == DashPhase.GLIDE && ctx.phaseTicks > 0 && player.horizontalCollision) ctx.fire(DashEvent.BLOCKED);
        else ctx.tick();

        Vec3 motion = player.getDeltaMovement();
        switch (ctx.phase) {
            case ANTICIPATION -> player.setDeltaMovement(motion.x * ANTICIPATION_DRAG, motion.y, motion.z * ANTICIPATION_DRAG);
            case GLIDE -> player.setDeltaMovement(ctx.glideMotion.x, glideLift(player, ctx, motion.y), ctx.glideMotion.z);
            case RECOVERY -> {
                if (ctx.phaseTicks == 0)
                    player.setDeltaMovement(ctx.glideMotion.x * RECOVERY_CARRY, motion.y, ctx.glideMotion.z * RECOVERY_CARRY);
            }
            default -> {}
        }
    }

    private static boolean isJumping(LocalPlayer player) {
        return player.input.jumping || (!player.onGround() && player.getDeltaMovement().y > 0);
    }

    private static double glideLift(Player player, DashContext ctx, double motionY) {
        if (!ctx.lift || ctx.phaseTicks != 0) return motionY;
        return Math.max(motionY, liftVelocity(ctx.startY + LIFT_HEIGHT - player.getY()));
    }

    private static double liftVelocity(double height) {
        double velocity = 0;
        while (apexHeight(velocity) < height) velocity += LIFT_STEP;
        return velocity;
    }

    private static double apexHeight(double velocity) {
        double height = 0;
        while (velocity > 0) {
            height += velocity;
            velocity = (velocity - GRAVITY) * AIR_DRAG;
        }
        return height;
    }

    private static void add3rdPartyDashStart(SimpleMessageEvent event)
    {
        String[] parts = event.getContent().split(",");
        if (parts.length < 2) return;
        int entityId, dir;
        try {
            entityId = Integer.parseInt(parts[0]);
            dir = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player) || entity == mc.player) return;
            DashContext ctx = CONTEXTS.computeIfAbsent(entityId, DashContext::new);
            ctx.reset();
            ctx.begin(DashDirection.byId(dir), entity.getYRot(), 0, DashManager.GLIDE_SPEED);
        });
    }

    private static void onResultMessage(SimpleMessageEvent event) {
        String[] parts = event.getContent().split(",");
        if (parts.length < 2 || !"0".equals(parts[1])) return;
        int seq;
        try {
            seq = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            DashContext ctx = getActive(mc.player);
            if (ctx != null && ctx.seq == seq) ctx.fire(DashEvent.REJECTED);
        });
    }
}
