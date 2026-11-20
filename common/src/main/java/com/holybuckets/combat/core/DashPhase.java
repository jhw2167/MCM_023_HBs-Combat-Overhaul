package com.holybuckets.combat.core;

public enum DashPhase {
    READY(0),
    ANTICIPATION(2),
    GLIDE(5),
    RECOVERY(4);

    public final int ticks;

    DashPhase(int ticks) {
        this.ticks = ticks;
    }

    public DashPhase update(DashEvent event)
    {
        return switch (this) {
            case READY -> event == DashEvent.REQUEST ? ANTICIPATION : READY;
            case ANTICIPATION -> switch (event) {
                case ELAPSED -> GLIDE;
                case INTERRUPTED, REJECTED -> READY;
                default -> ANTICIPATION;
            };
            case GLIDE -> switch (event) {
                case ELAPSED, BLOCKED, REJECTED -> RECOVERY;
                case INTERRUPTED -> READY;
                default -> GLIDE;
            };
            case RECOVERY -> switch (event) {
                case REQUEST -> ANTICIPATION;
                case ELAPSED, INTERRUPTED, REJECTED -> READY;
                default -> RECOVERY;
            };
        };
    }

    public int ticks(DashProfile profile) {
        return this == GLIDE ? profile.glideTicks() : ticks;
    }

    public int startTick(DashProfile profile) {
        return switch (this) {
            case READY, ANTICIPATION -> 0;
            case GLIDE -> ANTICIPATION.ticks;
            case RECOVERY -> ANTICIPATION.ticks + GLIDE.ticks(profile);
        };
    }

    public static int totalTicks(DashProfile profile) {
        return ANTICIPATION.ticks + GLIDE.ticks(profile) + RECOVERY.ticks;
    }
}
