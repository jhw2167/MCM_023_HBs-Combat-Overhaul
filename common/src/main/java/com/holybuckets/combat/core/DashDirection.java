package com.holybuckets.combat.core;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public enum DashDirection {
    FORWARD(0F, 1F, "dodge_back"),
    BACK(0F, -1F, "dodge_back"),
    LEFT(1F, 0F, "dodge_left"),
    RIGHT(-1F, 0F, "dodge_right");

    public static final String FALLBACK_ANIMATION = "dodge_back";

    public final float strafe;
    public final float forward;
    public final String animation;

    DashDirection(float strafe, float forward, String animation) {
        this.strafe = strafe;
        this.forward = forward;
        this.animation = animation;
    }

    public Vec3 toWorld(float yaw, double speed) {
        float sin = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(strafe * cos - forward * sin, 0.0D, forward * cos + strafe * sin).scale(speed);
    }

    public static DashDirection fromInput(boolean up, boolean down, boolean left, boolean right) {
        if (left != right) return left ? LEFT : RIGHT;
        if (up != down) return up ? FORWARD : BACK;
        return null;
    }

    public static DashDirection byId(int id) {
        DashDirection[] values = values();
        return (id < 0 || id >= values.length) ? FORWARD : values[id];
    }
}
