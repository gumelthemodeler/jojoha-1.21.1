package org.gumel.jojoha.stand.hierophant;

public final class HierophantClips {
    public static final String SHOTGUN = "hg_shotgun";
    public static final String SPIKE = "hg_spike";
    public static final String MINE = "hg_mine";
    public static final String RADIUS = "hg_radius";
    public static final String POSSESS = "hg_possess";
    public static final String POSSESS_ALT = "hg_possess_alt";
    public static final String POSSESS_EXIT = "hg_possess_exit";

    public static final int SHOTGUN_RELEASE_TICKS = 13;
    public static final int SPIKE_CONTACT_TICKS = 13;
    public static final int MINE_CONTACT_TICKS = 9;
    public static final int RADIUS_CAST_TICKS = 19;
    public static final int POSSESS_TICKS = 13;
    public static final int EXIT_HIDDEN_TICKS = 3;
    public static final int EXIT_EMERGE_TICKS = 7;

    public static final double POSSESS_DROP = 14.0 / 16.0;
    public static final double POSSESS_ALT_DROP = 3.0 / 16.0;

    private HierophantClips() {
    }
}
