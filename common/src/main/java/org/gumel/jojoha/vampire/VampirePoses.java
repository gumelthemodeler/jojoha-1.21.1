package org.gumel.jojoha.vampire;

import java.util.Set;

public final class VampirePoses {
    public static final String FLESH_BUD = "fleshbud";

    public static final String BLITZ = "blitz";

    public static final String SUBZERO = "subzero";

    public static final String SPACE_RIPPER = "space_ripper_stingy_eyes";
    public static final String SPACE_RIPPER_VARIANT = "space_ripper_stingy_eyes_variant";

    public static final String WALL_CRAWL_START = "wall_crawl_start";

    public static final String VAMPIRIC_DRIVE = "vampiric_drive";

    public static final int VAMPIRIC_DRIVE_CHARGE_TICKS = 22;

    public static final String DRAIN = "drain";

    public static final int DRAIN_CONTACT_TICKS = 6;

    public static final String ZOMBIFY = "zombify";

    public static final int ZOMBIFY_RAISE_TICKS = 31;

    public static final String AMALGAMATE = "amalgamate";

    public static final int AMALGAMATE_SPREAD_TICKS = 12;

    public static final int AMALGAMATE_FUSE_TICKS = 16;

    public static final Set<String> ALL = Set.of(FLESH_BUD, BLITZ, SUBZERO, SPACE_RIPPER, SPACE_RIPPER_VARIANT,
            WALL_CRAWL_START, "wall_crawl_loop", VAMPIRIC_DRIVE, DRAIN, ZOMBIFY, AMALGAMATE);

    private VampirePoses() {
    }
}
