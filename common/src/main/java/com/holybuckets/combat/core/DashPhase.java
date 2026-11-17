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

    public int startTick() {
        return switch (this) {
            case READY, ANTICIPATION -> 0;
            case GLIDE -> ANTICIPATION.ticks;
            case RECOVERY -> ANTICIPATION.ticks + GLIDE.ticks;
        };
    }

    public static int totalTicks() {
        return ANTICIPATION.ticks + GLIDE.ticks + RECOVERY.ticks;
    }
}
