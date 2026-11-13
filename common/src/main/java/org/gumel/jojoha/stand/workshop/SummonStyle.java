package org.gumel.jojoha.stand.workshop;

public enum SummonStyle {
    CLASSIC("classic", true, 8),
    STEP_OUT("step_out", true, 16),
    RISE("rise", false, 16),
    FLASH("flash", false, 2),
    MENACING("menacing", false, 20);

    public static final int CUE_SHIFT = 24;

    private final String key;
    private final boolean free;
    private final int emergeTicks;

    SummonStyle(String key, boolean free, int emergeTicks) {
        this.key = key;
        this.free = free;
        this.emergeTicks = emergeTicks;
    }

    public String key() {
        return key;
    }

    public boolean free() {
        return free;
    }

    public int emergeTicks() {
        return emergeTicks;
    }

    public static SummonStyle of(int index) {
        SummonStyle[] all = values();
        return index >= 0 && index < all.length ? all[index] : CLASSIC;
    }
}
