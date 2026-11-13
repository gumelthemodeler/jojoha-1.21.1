package org.gumel.jojoha.data;

import java.util.ArrayList;
import java.util.List;

public enum StatPerk {
    IRON_KNUCKLES(false, StatPoints.STRENGTH, 25, "iron_knuckles"),
    SHIELDBREAKER(false, StatPoints.STRENGTH, 50, "shieldbreaker"),
    STAGGERING_BLOW(false, StatPoints.STRENGTH, 75, "staggering_blow"),
    OVERWHELMING_FORCE(false, StatPoints.STRENGTH, 99, "overwhelming_force"),

    THICK_SKIN(false, StatPoints.VITALITY, 25, "thick_skin"),
    RECOVERY(false, StatPoints.VITALITY, 50, "recovery"),
    LAST_STAND(false, StatPoints.VITALITY, 75, "last_stand"),
    UNDYING(false, StatPoints.VITALITY, 99, "undying"),

    LIGHT_FEET(false, StatPoints.AGILITY, 25, "light_feet"),
    QUICK_RECOVERY(false, StatPoints.AGILITY, 50, "quick_recovery"),
    SLIP_AWAY(false, StatPoints.AGILITY, 75, "slip_away"),
    WIND_STEP(false, StatPoints.AGILITY, 99, "wind_step"),

    DEEP_LUNGS(false, StatPoints.ENDURANCE, 25, "deep_lungs"),
    BRACED(false, StatPoints.ENDURANCE, 50, "braced"),
    IRON_STOMACH(false, StatPoints.ENDURANCE, 75, "iron_stomach"),
    IMMOVABLE(false, StatPoints.ENDURANCE, 99, "immovable"),

    FATED(false, StatPoints.WORTHINESS, 25, "fated"),
    RECOGNISED(false, StatPoints.WORTHINESS, 50, "recognised"),
    CHOSEN(false, StatPoints.WORTHINESS, 75, "chosen"),
    HEAVENS_FAVOUR(false, StatPoints.WORTHINESS, 99, "heavens_favour"),

    HEAVY_STRIKES(true, StatPoints.POWER, 25, "heavy_strikes"),
    GUARD_CRUSHER(true, StatPoints.POWER, 50, "guard_crusher"),
    ARMOUR_CRUSHER(true, StatPoints.POWER, 75, "armour_crusher"),
    FINISHER(true, StatPoints.POWER, 99, "finisher"),

    QUICK_HANDS(true, StatPoints.SPEED, 25, "quick_hands"),
    SWIFT_STEP(true, StatPoints.SPEED, 50, "swift_step"),
    RELENTLESS(true, StatPoints.SPEED, 75, "relentless"),
    TIME_IS_SHORT(true, StatPoints.SPEED, 99, "time_is_short"),

    STEADY_FORM(true, StatPoints.STAND_ENDURANCE, 25, "steady_form"),
    ENDURING(true, StatPoints.STAND_ENDURANCE, 50, "enduring"),
    UNWAVERING_FORM(true, StatPoints.STAND_ENDURANCE, 75, "unwavering_form"),
    UNBREAKABLE_FORM(true, StatPoints.STAND_ENDURANCE, 99, "unbreakable_form"),

    FIRM_GUARD(true, StatPoints.PROTECTION, 25, "firm_guard"),
    REBUKE(true, StatPoints.PROTECTION, 50, "rebuke"),
    BULWARK(true, StatPoints.PROTECTION, 75, "bulwark"),
    IMPENETRABLE(true, StatPoints.PROTECTION, 99, "impenetrable"),

    DEEP_WELL(true, StatPoints.POTENTIAL, 25, "deep_well"),
    WELLSPRING(true, StatPoints.POTENTIAL, 50, "wellspring"),
    EFFICIENT(true, StatPoints.POTENTIAL, 75, "efficient"),
    BOUNDLESS(true, StatPoints.POTENTIAL, 99, "boundless");

    public static final int[] THRESHOLDS = {25, 50, 75, 99};

    private final boolean stand;
    private final int stat;
    private final int threshold;
    private final String key;

    StatPerk(boolean stand, int stat, int threshold, String key) {
        this.stand = stand;
        this.stat = stat;
        this.threshold = threshold;
        this.key = key;
    }

    public boolean stand() {
        return stand;
    }

    public int stat() {
        return stat;
    }

    public int threshold() {
        return threshold;
    }

    public String nameKey() {
        return "perk.jojoha." + key;
    }

    public String descriptionKey() {
        return "perk.jojoha." + key + ".desc";
    }

    public boolean has(JojohaPlayerData data) {
        return reached(level(data, stand, stat));
    }

    public boolean reached(int level) {
        return level >= threshold;
    }

    public static List<StatPerk> of(boolean stand, int stat) {
        List<StatPerk> perks = new ArrayList<>(THRESHOLDS.length);
        for (StatPerk perk : values()) {
            if (perk.stand == stand && perk.stat == stat) {
                perks.add(perk);
            }
        }
        return perks;
    }

    public static List<StatPerk> crossed(boolean stand, int stat, int before, int after) {
        List<StatPerk> reached = new ArrayList<>();
        for (StatPerk perk : of(stand, stat)) {
            if (before < perk.threshold && after >= perk.threshold) {
                reached.add(perk);
            }
        }
        return reached;
    }

    private static int level(JojohaPlayerData data, boolean stand, int stat) {
        if (stand) {
            return data.stand.isPresent() ? data.stand.stats()[stat] : 0;
        }
        return StatPoints.playerStat(data, stat);
    }
}
