package org.gumel.jojoha.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.accessory.Charms;
import org.gumel.jojoha.combat.ArmourCurve;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.Clackers;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.EnergySystem;
import org.gumel.jojoha.combat.ScytheSweep;
import org.gumel.jojoha.combat.StatusResistance;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.dream.DreamRealm;
import org.gumel.jojoha.entity.VampireSpawns;
import org.gumel.jojoha.entity.WillowSpawner;
import org.gumel.jojoha.fortune.Wicked;
import org.gumel.jojoha.hamon.HamonMastery;
import org.gumel.jojoha.hamon.moves.SunlightYellowOverdriveMove;
import org.gumel.jojoha.labyrinth.DungeonSpawnerBlockEntity;
import org.gumel.jojoha.labyrinth.TntBarrelBlock;
import org.gumel.jojoha.pillar.PillarBirth;
import org.gumel.jojoha.stand.DividedAttention;
import org.gumel.jojoha.stand.Spirit;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.anubis.Bearer;
import org.gumel.jojoha.stand.combat.BarrageDemolition;
import org.gumel.jojoha.stand.mob.MobStands;
import org.gumel.jojoha.stand.skill.TimeStopSystem;
import org.gumel.jojoha.trait.TraitEffects;
import org.gumel.jojoha.trait.Traits;
import org.gumel.jojoha.ultimate.AdaptiveFlightMove;
import org.gumel.jojoha.ultimate.UltimateStrengthMove;
import org.gumel.jojoha.vampire.moves.VaporousFreezeMove;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.gumel.jojoha.config.NumberSetting.Unit.HARDNESS;
import static org.gumel.jojoha.config.NumberSetting.Unit.HEALTH;
import static org.gumel.jojoha.config.NumberSetting.Unit.ONE_IN;
import static org.gumel.jojoha.config.NumberSetting.Unit.PERCENT;
import static org.gumel.jojoha.config.NumberSetting.Unit.TIMES;
import static org.gumel.jojoha.config.NumberSetting.Unit.WHOLE;

public final class JojohaServerConfig {
    public static final ConfigFile FILE = new ConfigFile("jojoha-server.json", true)
            .ordered("damage", "standDamage", "combat", "statuses", "stand", "powers", "items", "loot", "world", "events");

    private static final Map<String, NumberSetting> LOOT = new LinkedHashMap<>();

    public static final ChoiceSetting<BlockBreaking> BLOCK_BREAKING = FILE.choice("stand", "standBlockBreaking",
            "block_breaking", BlockBreaking.class, BlockBreaking.ALWAYS);
    public static final NumberSetting HARDEST_BREAKABLE = FILE.number("stand", "hardestBreakable",
            "hardest_breakable", BarrageDemolition.HARDNESS_CAP, 0F, 50F, 0.5F, HARDNESS);
    public static final NumberSetting BLOCKS_PER_BLOW = FILE.number("stand", "blocksPerBlow",
            "blocks_per_blow", BarrageDemolition.MAX_PER_BEAT, 1F, 8F, 1F, WHOLE);

    public static final NumberSetting MOB_STAND_SPAWN = FILE.number("world", "mobStandSpawnChance",
            "mob_stand_spawn", MobStands.SPAWN_CHANCE * 100F, 0F, 10F, 0.1F, PERCENT);
    public static final NumberSetting STAND_USER_PULL = FILE.number("world", "standUserPull",
            "stand_user_pull", MobStands.USER_PULL, 1F, 10F, 0.5F, TIMES);
    public static final NumberSetting STAND_MOB_HEALTH = FILE.number("world", "standMobHealth",
            "stand_mob_health", MobStands.TOUGHER_HEALTH, 0F, 200F, 10F, PERCENT);
    public static final NumberSetting STAND_MOB_ARMOR = FILE.number("world", "standMobArmor",
            "stand_mob_armor", MobStands.TOUGHER_ARMOR, 0F, 20F, 1F, WHOLE);
    public static final NumberSetting ANUBIS_BEARER = FILE.number("world", "anubisBearerOneIn",
            "anubis_bearer", Bearer.CHANCE_IN, 0F, 5000F, 50F, ONE_IN);
    public static final NumberSetting VAMPIRE_WEIGHT = FILE.number("world", "vampireSpawnWeight",
            "vampire_weight", VampireSpawns.WEIGHT, 0F, 50F, 1F, WHOLE);
    public static final NumberSetting WILLOW_CHANCE = FILE.number("world", "willowSpawnChance",
            "willow_chance", WillowSpawner.ATTEMPT_CHANCE * 100F, 0F, 100F, 5F, PERCENT);
    public static final NumberSetting DREAM_CHANCE = FILE.number("world", "dreamChance",
            "dream_chance", DreamRealm.CHANCE * 100F, 0F, 100F, 1F, PERCENT);

    public static final NumberSetting FIRST_EVENT_DAY = FILE.number("events", "firstEventDay",
            "first_event_day", 7F, 0F, 100F, 1F, WHOLE);
    public static final ToggleSetting VAMPIRE_NIGHT = FILE.toggle("events", "vampireNight", "vampire_night", true);
    public static final NumberSetting VAMPIRE_NIGHT_ODDS = FILE.number("events", "vampireNightOneIn",
            "vampire_night_odds", 8F, 1F, 50F, 1F, ONE_IN);
    public static final ToggleSetting METEOR_SHOWER = FILE.toggle("events", "meteorShower", "meteor_shower", true);
    public static final NumberSetting METEOR_SHOWER_ODDS = FILE.number("events", "meteorShowerOneIn",
            "meteor_shower_odds", 10F, 1F, 50F, 1F, ONE_IN);
    public static final ToggleSetting VAMPIRE_RAID = FILE.toggle("events", "vampireRaid", "vampire_raid", true);
    public static final NumberSetting VAMPIRE_RAID_ODDS = FILE.number("events", "vampireRaidOneIn",
            "vampire_raid_odds", 12F, 1F, 50F, 1F, ONE_IN);
    public static final ToggleSetting STAND_USER_APPROACH = FILE.toggle("events", "standUserApproach",
            "stand_user_approach", true);
    public static final ToggleSetting SKIRMISHES = FILE.toggle("events", "skirmishes", "skirmishes", true);

    public static final NumberSetting PLAYER_AWAKENING = FILE.number("powers", "playerArrowOdds",
            "player_awakening", 1F, 0F, 4F, 0.05F, TIMES);
    public static final NumberSetting MOB_AWAKENING = FILE.number("powers", "mobArrowOdds",
            "mob_awakening", 1F, 0F, 4F, 0.05F, TIMES);
    public static final NumberSetting LEGENDARY_TRAIT = FILE.number("powers", "legendaryTraitChance",
            "legendary_trait", Traits.LEGENDARY_CHANCE * 100F, 0F, 100F, 0.5F, PERCENT);
    public static final NumberSetting BIRTHMARK = FILE.number("powers", "birthmarkChance",
            "birthmark", TraitEffects.BIRTHMARK_CHANCE * 100F, 0F, 100F, 0.5F, PERCENT);
    public static final NumberSetting PILLAR_START = FILE.number("powers", "pillarManStartChance",
            "pillar_start", PillarBirth.START_CHANCE * 100F, 0F, 100F, 0.5F, PERCENT);
    public static final ToggleSetting FORTUNES = FILE.toggle("powers", "fortunes", "fortunes", true);
    public static final ToggleSetting LIMB_LOSS = FILE.toggle("powers", "limbLoss", "limb_loss", true);
    public static final ToggleSetting SUPPORTER_PERKS = FILE.toggle("world", "supporterPerks",
            "supporter_perks", true);
    public static final NumberSetting WICKED_TREMBLE = FILE.number("powers", "wickedTrembleTicks",
            "wicked_tremble", Wicked.TREMBLE_TICKS, 0F, 100F, 5F, WHOLE);
    public static final NumberSetting WICKED_RADIUS = FILE.number("powers", "wickedTrembleRadius",
            "wicked_radius", (float) Wicked.TREMBLE_RADIUS, 0F, 24F, 1F,
            NumberSetting.Unit.BLOCKS);

    public static final NumberSetting SCYTHE_DAMAGE = FILE.number("combat", "scytheSweepDamage",
            "scythe_damage", ScytheSweep.DAMAGE, 0F, 10F, 0.5F,
            NumberSetting.Unit.HEALTH);
    public static final NumberSetting SCYTHE_RADIUS = FILE.number("combat", "scytheSweepRadius",
            "scythe_radius", ScytheSweep.RADIUS, 1F, 6F, 0.5F,
            NumberSetting.Unit.BLOCKS);
    public static final NumberSetting SCYTHE_COOLDOWN = FILE.number("combat", "scytheSweepCooldownTicks",
            "scythe_cooldown", ScytheSweep.COOLDOWN_TICKS, 10F, 200F, 5F, WHOLE);

    public static final NumberSetting BURN_DAMAGE = FILE.number("statuses", "burningDamagePerSecond",
            "burn_damage", Burning.DAMAGE, 0F, 4F, 0.5F,
            NumberSetting.Unit.HEALTH);
    public static final NumberSetting BURN_TICKS = FILE.number("statuses", "burningTicks",
            "burn_ticks", Burning.TICKS, 20F, 300F, 10F, WHOLE);
    public static final NumberSetting STAGGER_TICKS = FILE.number("statuses", "staggerTicks",
            "stagger_ticks", Stun.STAGGER_TICKS, 0F, 60F, 1F, WHOLE);
    public static final NumberSetting SYO_LOCK_TICKS = FILE.number("statuses", "sunlightYellowStunTicks",
            "syo_lock_ticks", SunlightYellowOverdriveMove.LOCK_TICKS, 0F, 100F, 5F, WHOLE);
    public static final NumberSetting STUN_REPEAT = FILE.number("statuses", "repeatStunPercent",
            "stun_repeat", StatusResistance.REPEAT_PERCENT, 0F, 100F, 5F, PERCENT);
    public static final NumberSetting STUN_REPEAT_WINDOW = FILE.number("statuses", "repeatStunWindowTicks",
            "stun_repeat_window", StatusResistance.REPEAT_WINDOW_TICKS, 0F, 200F, 10F, WHOLE);
    public static final NumberSetting BOSS_STATUS = FILE.number("statuses", "bossStatusPercent",
            "boss_status", StatusResistance.BOSS_PERCENT, 0F, 100F, 5F, PERCENT);
    public static final ToggleSetting MOB_STATUSES = FILE.toggle("statuses", "mobsInflictStatuses",
            "mob_statuses", true);
    public static final NumberSetting VAPOROUS_FREEZE_TICKS = FILE.number("statuses", "vaporousFreezeTicks",
            "vaporous_freeze_ticks", VaporousFreezeMove.FREEZE_TICKS, 0F, 300F, 10F, WHOLE);

    public static final NumberSetting ULTIMATE_STRENGTH_LEVEL = FILE.number("powers", "ultimateStrengthLevel",
            "ultimate_strength_level", UltimateStrengthMove.LEVEL, 1F, 10F, 1F, WHOLE);
    public static final NumberSetting ULTIMATE_STRENGTH_SECONDS = FILE.number("powers", "ultimateStrengthSeconds",
            "ultimate_strength_seconds", UltimateStrengthMove.SECONDS, 1F, 60F, 1F, WHOLE);
    public static final NumberSetting WING_DRAIN = FILE.number("powers", "wingFlightDrainPerSecond",
            "wing_drain", AdaptiveFlightMove.DRAIN_PER_SECOND, 0F, 30F, 0.5F, WHOLE);
    public static final NumberSetting WING_FLAP_COST = FILE.number("powers", "wingFlapCost",
            "wing_flap_cost", AdaptiveFlightMove.FLAP_COST, 0F, 30F, 0.5F, WHOLE);

    public static final NumberSetting STAR_MIRROR = FILE.number("items", "starMirrorTicks",
            "star_mirror", Charms.STAR_MIRROR_TICKS, 0F, 10F, 1F, WHOLE);
    public static final NumberSetting PARRY_LAG_ALLOWANCE = FILE.number("combat", "parryLagAllowanceTicks",
            "parry_lag_allowance", Defence.LAG_ALLOWANCE_TICKS, 0F, 10F, 1F, WHOLE);
    public static final NumberSetting LAST_STAND = FILE.number("combat", "lastStandHealthPercent",
            "last_stand", Defence.LAST_STAND_PERCENT, 0F, 100F, 5F, PERCENT);
    public static final NumberSetting SHOULDER_PLATE = FILE.number("items", "fracturedShoulderPlate",
            "shoulder_plate", Charms.SHOULDER_PLATE_PERCENT, 0F, 50F, 1F, PERCENT);
    public static final NumberSetting DEFENSE_SKILL_COST = FILE.number("stand", "defenseSkillCost",
            "defense_skill_cost", DividedAttention.SKILL_COST_PERCENT, 0F, 100F, 5F, PERCENT);
    public static final NumberSetting DEFENSE_PUNCH_SLOW = FILE.number("stand", "defensePunchSlow",
            "defense_punch_slow", DividedAttention.PUNCH_SLOW, 1F, 3F, 0.1F, TIMES);
    public static final NumberSetting HALF_SUMMON_RECOVERY = FILE.number("stand", "halfSummonRecoverySeconds",
            "half_summon_recovery", EnergySystem.RECOVERY_SECONDS, 0F, 600F, 10F, WHOLE);
    public static final ToggleSetting MAGICIAN_FLAMES = FILE.toggle("stand", "magicianFlames",
            "magician_flames", true);
    public static final NumberSetting TIME_STOPPER_MOVES = FILE.number("combat", "timeStopperMoveTicks",
            "time_stopper_moves", TimeStopSystem.TIME_STOPPER_MOVE_TICKS, 0F, 200F, 5F, WHOLE);
    public static final NumberSetting STAND_DRAIN_COMBAT = FILE.number("stand", "standDrainCombatSeconds",
            "stand_drain_combat", EnergySystem.STAND_DRAIN_COMBAT_SECONDS, 20F, 900F, 10F, WHOLE);
    public static final NumberSetting STAND_DRAIN_IDLE = FILE.number("stand", "standDrainIdleSeconds",
            "stand_drain_idle", EnergySystem.STAND_DRAIN_IDLE_SECONDS, 20F, 1800F, 10F, WHOLE);
    public static final NumberSetting SKULL_OF_FORTUNE = FILE.number("items", "skullOfFortune",
            "skull_of_fortune", Charms.SKULL_SCALE, 1F, 3F, 0.1F, TIMES);

    public static final NumberSetting STAND_PLACEMENT_RANGE = FILE.number("stand", "standPlacementRange",
            "stand_placement_range", 1.5F, 0F, 3F, 0.1F, NumberSetting.Unit.BLOCKS);
    public static final ToggleSetting CUSTOM_IDLES = FILE.toggle("stand", "customIdles",
            "custom_idles", true);
    public static final ToggleSetting CHIPS_ANY_ARMOR = FILE.toggle("items", "soulChipsOnAnyArmor",
            "chips_any_armor", false);
    public static final NumberSetting SECOND_HAND = FILE.number("powers", "secondHandChance",
            "second_hand", TraitEffects.SECOND_HAND_CHANCE * 100F, 0F, 100F, 1F, PERCENT);
    public static final NumberSetting SPIRIT_LEAN = FILE.number("powers", "spiritLean",
            "spirit_lean", Spirit.LEAN, 0F, 12F, 0.5F, TIMES);
    public static final NumberSetting STAND_SPREAD = FILE.number("powers", "standSpread",
            "stand_spread", Spirit.SPREAD, 0F, 4F, 0.25F, TIMES);
    public static final NumberSetting HERMIT_MASTERY = FILE.number("powers", "hermitPurpleMastery",
            "hermit_mastery", HamonMastery.FULL_BARS, 0F, 500F, 5F, WHOLE);

    public static final NumberSetting LOOT_STAND_ARROWS = loot("standArrows", "stand_arrows");
    public static final NumberSetting LOOT_ARROWHEADS = loot("arrowheads", "arrowheads");
    public static final NumberSetting LOOT_REQUIEM = loot("requiemArrow", "requiem_arrow");
    public static final NumberSetting LOOT_STONE_MASKS = loot("stoneMasks", "stone_masks");
    public static final NumberSetting LOOT_PATH_SCROLLS = loot("pathScrolls", "path_scrolls");
    public static final NumberSetting LOOT_FISHING = loot("fishingArrows", "fishing_arrows");
    public static final NumberSetting LOOT_LABYRINTH_KEYS = loot("labyrinthKeys", "labyrinth_keys");
    public static final NumberSetting LOOT_ANUBIS = loot("anubis", "anubis");
    public static final NumberSetting LOOT_STAT_CRYSTALS = loot("statCrystals", "stat_crystals");
    public static final NumberSetting LOOT_ACCESSORIES = loot("accessories", "accessories");
    public static final NumberSetting LOOT_BIZARRE_FINDS = loot("bizarreFinds", "bizarre_finds");
    public static final NumberSetting LOOT_CURIOS = loot("curios", "curios");

    public static final NumberSetting STAND_DAMAGE = damage("stand", "stand_damage");
    public static final NumberSetting HAMON_DAMAGE = damage("hamon", "hamon_damage");
    public static final NumberSetting VAMPIRE_DAMAGE = damage("vampire", "vampire_damage");
    public static final NumberSetting COMBAT_DAMAGE = damage("combat", "combat_damage");
    public static final NumberSetting MOB_STAND_DAMAGE = damage("mobStands", "mob_stand_damage");
    public static final NumberSetting VAMPIRE_MOB_DAMAGE = damage("vampireMobs", "vampire_mob_damage");
    public static final NumberSetting PLAYER_DAMAGE = damage("againstPlayers", "player_damage");
    public static final ToggleSetting PVP_ARMOUR_CURVE = FILE.toggle("damage", "pvpArmourCurve",
            "pvp_armour_curve", true);
    public static final NumberSetting PVP_ARMOUR_FLOOR = FILE.number("damage", "modHitFloorPercent",
            "pvp_armour_floor", ArmourCurve.FLOOR, 0F, 100F, 5F, PERCENT);
    public static final NumberSetting MAX_HIT = FILE.number("damage", "maxPerHit",
            "max_per_hit", 0F, 0F, 40F, 0.5F, HEALTH);

    public static final NumberSetting CLACKER_THROW_DAMAGE = FILE.number("combat", "clackerThrowDamage",
            "clacker_throw_damage", Clackers.THROW_DAMAGE, 0F, 20F, 0.5F, HEALTH);
    public static final NumberSetting CLACKER_SPIN_DAMAGE = FILE.number("combat", "clackerSpinDamage",
            "clacker_spin_damage", Clackers.SPIN_DAMAGE, 0F, 20F, 0.5F, HEALTH);

    public static final NumberSetting SPAWNER_WAVE_SIZE = FILE.number("world", "spawnerWaveSize",
            "spawner_wave_size", DungeonSpawnerBlockEntity.WAVE_SIZE, 1F, 12F, 1F, WHOLE);
    public static final NumberSetting SPAWNER_WAVE_DELAY = FILE.number("world", "spawnerWaveDelaySeconds",
            "spawner_wave_delay", DungeonSpawnerBlockEntity.WAVE_DELAY_SECONDS, 0F, 120F, 1F, WHOLE);
    public static final NumberSetting SPAWNER_RANGE = FILE.number("world", "spawnerRange",
            "spawner_range", DungeonSpawnerBlockEntity.RANGE, 4F, 48F, 1F, WHOLE);
    public static final NumberSetting SPAWNER_WAVES = FILE.number("world", "spawnerWaves",
            "spawner_waves", DungeonSpawnerBlockEntity.WAVES, 1F, 20F, 1F, WHOLE);
    public static final NumberSetting BARREL_DAMAGE = FILE.number("world", "tntBarrelDamage",
            "tnt_barrel_damage", TntBarrelBlock.MAX_DAMAGE, 0F, 40F, 0.5F, HEALTH);

    private static final Map<ResourceLocation, NumberSetting> PER_STAND = new LinkedHashMap<>();

    static {
        CLACKER_THROW_DAMAGE.formerly("throwDamage");
        CLACKER_SPIN_DAMAGE.formerly("spinDamage");
    }

    private JojohaServerConfig() {
    }

    public static void bootstrap() {
        for (ResourceLocation id : StandTypes.all().keySet()) {
            if (PER_STAND.containsKey(id) || StandTypes.secret(id)) {
                continue;
            }
            NumberSetting setting = FILE.number("standDamage", id.toString(), "stand_damage_for",
                    1F, 0F, 5F, 0.05F, TIMES);
            setting.withLabelArgument("stand." + id.getNamespace() + "." + id.getPath());
            PER_STAND.put(id, setting);
        }
    }

    private static NumberSetting loot(String key, String id) {
        NumberSetting setting = FILE.number("loot", key, "loot_" + id, 1F, 0F, 5F, 0.1F, TIMES);
        LOOT.put(id, setting);
        return setting;
    }

    public static NumberSetting loot(String id) {
        return LOOT.get(id);
    }

    private static NumberSetting damage(String key, String langKey) {
        return FILE.number("damage", key, langKey, 1F, 0F, 5F, 0.05F, TIMES);
    }

    public static Map<ResourceLocation, NumberSetting> perStand() {
        return Collections.unmodifiableMap(PER_STAND);
    }

    public static float standDamage(ResourceLocation stand) {
        NumberSetting setting = stand == null ? null : PER_STAND.get(stand);
        return setting == null ? 1F : setting.value();
    }

    public static boolean standsMayBreak(ServerLevel level) {
        return BLOCK_BREAKING.get().allows(level);
    }

    public static boolean oneIn(NumberSetting setting, RandomSource random) {
        int odds = setting.whole();
        return odds > 0 && random.nextInt(odds) == 0;
    }

    public static float awakening(float chance, boolean mob) {
        return Math.min(1F, chance * (mob ? MOB_AWAKENING : PLAYER_AWAKENING).value());
    }
}
