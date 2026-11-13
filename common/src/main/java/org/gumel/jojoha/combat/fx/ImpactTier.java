package org.gumel.jojoha.combat.fx;

public enum ImpactTier {
    TAP(0, 4, 0F, 0F, false, false, false),

    SOLID(0, 5, 0.9F, 0F, true, false, false),

    HEAVY(100, 7, 2.2F, 0.035F, true, true, true),

    FINISHER(200, 9, 3.4F, 0.06F, true, true, true),

    GUARD_BREAK(100, 7, 1.6F, 0.025F, true, false, true),

    PARRY(200, 6, 0F, 0F, true, false, true),

    THUD(50, 5, 1.3F, 0.012F, true, false, false);

    private final int hitStopMs;
    private final int frames;
    private final float kickDegrees;
    private final float fovPunch;
    private final boolean shockwave;
    private final boolean streaks;
    private final boolean shards;

    ImpactTier(int hitStopMs, int frames, float kickDegrees, float fovPunch,
               boolean shockwave, boolean streaks, boolean shards) {
        this.hitStopMs = hitStopMs;
        this.frames = frames;
        this.kickDegrees = kickDegrees;
        this.fovPunch = fovPunch;
        this.shockwave = shockwave;
        this.streaks = streaks;
        this.shards = shards;
    }

    public int hitStopMs() {
        return hitStopMs;
    }

    public int frames() {
        return frames;
    }

    public float kickDegrees() {
        return kickDegrees;
    }

    public float fovPunch() {
        return fovPunch;
    }

    public boolean shockwave() {
        return shockwave;
    }

    public boolean streaks() {
        return streaks;
    }

    public boolean shards() {
        return shards;
    }

    public static ImpactTier byId(int id) {
        ImpactTier[] all = values();
        return id >= 0 && id < all.length ? all[id] : TAP;
    }
}
