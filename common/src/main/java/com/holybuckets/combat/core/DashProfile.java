package com.holybuckets.combat.core;

import net.minecraft.util.Mth;

public enum DashProfile {
    NORMAL(DashManager.DASH_DISTANCE, ""),
    SLOW(DashManager.SLOW_DASH_DISTANCE, "slow_");

    public final double distance;
    public final String animationPrefix;

    DashProfile(double distance, String animationPrefix) {
        this.distance = distance;
        this.animationPrefix = animationPrefix;
    }

    public double glideDuration() {
        return distance / DashManager.GLIDE_SPEED;
    }

    public int glideTicks() {
        return Mth.ceil(glideDuration() - 1.0E-6D);
    }

    public double glideScale(int phaseTicks) {
        return Mth.clamp(glideDuration() - phaseTicks, 0.0D, 1.0D);
    }

    public static DashProfile byId(int id) {
        DashProfile[] values = values();
        return (id < 0 || id >= values.length) ? NORMAL : values[id];
    }
}
