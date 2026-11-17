package com.holybuckets.combat.core;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class DashContext {

    public final int entityId;
    public DashPhase phase = DashPhase.READY;
    public DashDirection direction = DashDirection.FORWARD;
    public Vec3 glideMotion = Vec3.ZERO;
    public int phaseTicks;
    public int seq;

    public DashContext(int entityId) {
        this.entityId = entityId;
    }

    public boolean canRequest() {
        return phase == DashPhase.READY || phase == DashPhase.RECOVERY;
    }

    public boolean begin(DashDirection direction, float yaw, int seq, double speed) {
        if (!canRequest()) return false;
        this.direction = direction;
        this.seq = seq;
        this.glideMotion = direction.toWorld(yaw, speed);
        return fire(DashEvent.REQUEST);
    }

    public void reset() {
        phase = DashPhase.READY;
        phaseTicks = 0;
    }

    public boolean fire(DashEvent event) {
        DashPhase next = phase.update(event);
        if (next == phase) return false;
        phase = next;
        phaseTicks = 0;
        return true;
    }

    public boolean tick() {
        if (phase == DashPhase.READY) return false;
        phaseTicks++;
        return phaseTicks >= phase.ticks && fire(DashEvent.ELAPSED);
    }

    public boolean isActive() {
        return phase != DashPhase.READY;
    }

    public boolean isMoving() {
        return phase == DashPhase.ANTICIPATION || phase == DashPhase.GLIDE;
    }

    public float phaseProgress(float partialTick) {
        if (phase.ticks == 0) return 1.0F;
        return Mth.clamp((phaseTicks + partialTick) / phase.ticks, 0.0F, 1.0F);
    }

    public float timeline(float partialTick) {
        if (phase == DashPhase.READY) return DashPhase.totalTicks();
        return phase.startTick() + Math.min(phase.ticks, phaseTicks + partialTick);
    }
}
