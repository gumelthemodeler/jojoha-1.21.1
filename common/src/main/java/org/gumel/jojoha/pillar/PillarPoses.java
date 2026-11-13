package org.gumel.jojoha.pillar;

import java.util.Set;

public final class PillarPoses {
    public static final String RIB_BLADES = "rib_blades";
    public static final String DIVINE_SANDSTORM = "divine_sandstorm";
    public static final String TORNADO = "tornado";
    public static final String ATMOSPHERIC_RIFT = "atmospheric_rift";
    public static final String GALE_CUT = "gale_cut";
    public static final String WIND_CLOAK = "wind_cloak";
    public static final String BLOOD_BOIL = "blood_boil";
    public static final String BOILING_BLOOD_NEEDLES = "boiling_blood_needles";
    public static final String VESSEL_GRAB = "vessel_grab";
    public static final String CRYING_VENT = "crying_vent";
    public static final String HEAT_BODY = "heat_body";
    public static final String BRILLIANT_BLADES = "brilliant_blades";
    public static final String SAW_GRIP = "saw_grip";
    public static final String SHINING_SLASH = "shining_slash";
    public static final String BLINDING_FLASH = "blinding_flash";
    public static final String ULTIMATE_STRENGTH = "ultimate_strength";

    public static final int RIB_BLADES_CONTACT_TICKS = 10;
    public static final int DIVINE_SANDSTORM_CONTACT_TICKS = 12;
    public static final int TORNADO_RELEASE_TICKS = 25;
    public static final int ATMOSPHERIC_RIFT_RELEASE_TICKS = 17;
    public static final int GALE_CUT_RELEASE_TICKS = 9;
    public static final int WIND_CLOAK_REVEAL_TICKS = 7;
    public static final int BLOOD_BOIL_RELEASE_TICKS = 6;
    public static final int BOILING_BLOOD_NEEDLES_CONTACT_TICKS = 5;
    public static final int VESSEL_GRAB_CONTACT_TICKS = 7;
    public static final int CRYING_VENT_RELEASE_TICKS = 25;
    public static final int HEAT_BODY_REVEAL_TICKS = 6;
    public static final int BRILLIANT_BLADES_REVEAL_TICKS = 6;
    public static final int SAW_GRIP_CONTACT_TICKS = 9;
    public static final int SHINING_SLASH_CONTACT_TICKS = 13;
    public static final int BLINDING_FLASH_RELEASE_TICKS = 8;
    public static final int ULTIMATE_STRENGTH_FLEX_TICKS = 9;

    public static final Set<String> ALL = Set.of(RIB_BLADES, DIVINE_SANDSTORM, TORNADO, ATMOSPHERIC_RIFT, GALE_CUT,
            WIND_CLOAK, BLOOD_BOIL, BOILING_BLOOD_NEEDLES, VESSEL_GRAB, CRYING_VENT, HEAT_BODY, BRILLIANT_BLADES,
            SAW_GRIP, SHINING_SLASH, BLINDING_FLASH, ULTIMATE_STRENGTH);

    private PillarPoses() {
    }
}
