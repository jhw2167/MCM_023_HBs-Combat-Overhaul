package com.holybuckets.combat.core;

import com.holybuckets.combat.Constants;
import com.holybuckets.combat.config.ModConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.event.custom.ServerTickEvent;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;
import com.holybuckets.foundation.event.custom.TickType;
import com.holybuckets.foundation.networking.SimpleStringMessage;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DashManager  {

    public static final String MSG_REQUEST = Constants.MOD_ID + ":dash_request";
    public static final String MSG_RESULT = Constants.MOD_ID + ":dash_result";
    public static final String MSG_START = Constants.MOD_ID + ":dash_start";

    public static final double DASH_DISTANCE = 6.0D;
    public static final double SLOW_DASH_DISTANCE = 3.0D;
    public static final double GLIDE_SPEED = DASH_DISTANCE / DashPhase.GLIDE.ticks;
    public static final double BROADCAST_RANGE = 64.0D;
    public static final float SOUND_VOLUME = 0.6F;
    public static final float SOUND_PITCH = 1.6F;

    private static final Map<ServerPlayer, DashContext> CONTEXTS = new HashMap<>();

    public static void init(EventRegistrar reg) {
        reg.registerOnSimpleMessage(MSG_REQUEST, DashManager::onRequest);
        reg.registerOnServerTick(TickType.ON_SINGLE_TICK, DashManager::onServerTick);
        reg.registerOnServerStopped(event -> CONTEXTS.clear());
    }

    public static boolean canDash(Player player) {
        return player != null
            && player.isAlive()
            && !player.isSpectator()
            && !player.isPassenger()
            && !player.isFallFlying()
            && !player.isSleeping()
            && !player.isInWater()
            && !player.isInLava()
            && !player.getAbilities().flying;
    }

    public static int cooldownTicks(DashProfile profile) {
        return DashPhase.ANTICIPATION.ticks + DashPhase.GLIDE.ticks(profile);
    }

    public static DashProfile profileFor(Player player) {
        return isSlowed(player) ? DashProfile.SLOW : DashProfile.NORMAL;
    }

    public static boolean isSlowed(Player player) {
        if (player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) return true;
        return ModConfig.getInstance().isSlowBlock(player.level().getBlockState(player.getOnPos()).getBlock());
    }

    private static void onRequest(SimpleMessageEvent event)
    {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        String[] parts = event.getContent().split(",");
        if (parts.length < 3) return;

        int dir, seq, invSlot;
        try {
            dir = Integer.parseInt(parts[0]);
            seq = Integer.parseInt(parts[1]);
            invSlot = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) return;
        server.execute(() -> handleRequest(player, DashDirection.byId(dir), seq, invSlot));
    }

    private static void handleRequest(ServerPlayer player, DashDirection direction, int seq, int invSlotId)
    {
        if (invSlotId < 0 || invSlotId >= player.getInventory().getContainerSize()) {
            SimpleStringMessage.createAndFire(player, MSG_RESULT, seq + ",0");
            return;
        }
        ItemStack stack = player.getInventory().getItem(invSlotId);
        DashContext ctx = CONTEXTS.get(player);
        boolean valid = canDash(player)
            && ModConfig.getInstance().isDashItem(stack.getItem())
            && (ctx==null || ctx.canRequest());

        if (!valid) {
            SimpleStringMessage.createAndFire(player, MSG_RESULT, seq + ",0");
            return;
        }

        if (ctx==null) {
            ctx = new DashContext(player.getId());
            CONTEXTS.put(player, ctx);
        }
        DashProfile profile = profileFor(player);
        ctx.begin(direction, profile, player.getYRot(), seq, GLIDE_SPEED);

        if (!player.getAbilities().instabuild) stack.shrink(1);
        player.fallDistance = 0.0F;

        //Need a better sound, some sort of whoosh that isnt the firecracker
        //player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
            //SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, SOUND_VOLUME, SOUND_PITCH);
        broadcastStart(player, direction, profile);
    }

    private static void broadcastStart(ServerPlayer player, DashDirection direction, DashProfile profile) {
        String payload = player.getId() + "," + direction.ordinal() + "," + profile.ordinal();
        double rangeSqr = BROADCAST_RANGE * BROADCAST_RANGE;
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other == player || other.distanceToSqr(player) > rangeSqr) continue;
            SimpleStringMessage.createAndFire(other, MSG_START, payload);
        }
    }

    private static void onServerTick(ServerTickEvent event) {
        if (CONTEXTS.isEmpty()) return;
        Iterator<Map.Entry<ServerPlayer, DashContext>> it = CONTEXTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ServerPlayer, DashContext> entry = it.next();
            ServerPlayer player = entry.getKey();
            DashContext ctx = entry.getValue();

            if (player.isRemoved() || !canDash(player)) ctx.fire(DashEvent.INTERRUPTED);
            else ctx.tick();

            if (ctx.phase == DashPhase.GLIDE || ctx.phase == DashPhase.RECOVERY) player.fallDistance = 0.0F;
            if (!ctx.isActive()) it.remove();
        }
    }


}
