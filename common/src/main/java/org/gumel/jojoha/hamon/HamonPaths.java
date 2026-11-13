package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.hamon.moves.BottleCapShotMove;
import org.gumel.jojoha.hamon.moves.BubbleCutterMove;
import org.gumel.jojoha.hamon.moves.BubbleLauncherMove;
import org.gumel.jojoha.hamon.moves.ChannelingMove;
import org.gumel.jojoha.hamon.moves.ClackerVolleyMove;
import org.gumel.jojoha.hamon.moves.ForcedRippleBreathingMove;
import org.gumel.jojoha.hamon.moves.HamonOverdriveMove;
import org.gumel.jojoha.hamon.moves.NextLineMove;
import org.gumel.jojoha.hamon.moves.RippleBarrierMove;
import org.gumel.jojoha.hamon.moves.RipplePulseMove;
import org.gumel.jojoha.hamon.moves.RippleRebukeMove;
import org.gumel.jojoha.hamon.moves.RippleWalkMove;
import org.gumel.jojoha.hamon.moves.ScarletOverdriveMove;
import org.gumel.jojoha.hamon.moves.SnakeMufflerMove;
import org.gumel.jojoha.hamon.moves.SunlightYellowOverdriveMove;
import org.gumel.jojoha.hamon.moves.ThunderCrossSplitAttackMove;
import org.gumel.jojoha.hamon.moves.TurquoiseBlueOverdriveMove;
import org.gumel.jojoha.hamon.moves.YouthfulBreathMove;
import org.gumel.jojoha.hamon.moves.ZoomPunchMove;
import org.gumel.jojoha.pillar.moves.AtmosphericRiftMove;
import org.gumel.jojoha.pillar.moves.BladeFlashMove;
import org.gumel.jojoha.pillar.moves.BoilingBloodNeedlesMove;
import org.gumel.jojoha.pillar.moves.BrilliantBladesMove;
import org.gumel.jojoha.pillar.moves.CryingVentMove;
import org.gumel.jojoha.pillar.moves.DivineSandstormMove;
import org.gumel.jojoha.pillar.moves.GaleCutMove;
import org.gumel.jojoha.pillar.moves.HeatBodyMove;
import org.gumel.jojoha.pillar.moves.SawGripMove;
import org.gumel.jojoha.pillar.moves.ShiningSlashMove;
import org.gumel.jojoha.pillar.moves.TornadoMove;
import org.gumel.jojoha.pillar.moves.VesselGrabMove;
import org.gumel.jojoha.pillar.moves.WindCloakMove;
import org.gumel.jojoha.vampire.moves.BloodBoilMove;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HamonPaths {
    private static final Map<ResourceLocation, HamonPath> PATHS = new LinkedHashMap<>();

    public static final HamonPath HERMIT = register("hermit", "Hermit Path", null, List.of(
            RipplePulseMove.ID,
            ZoomPunchMove.ID,
            HamonOverdriveMove.ID,
            RippleBarrierMove.ID,
            SunlightYellowOverdriveMove.ID,
            ChannelingMove.ID));
    public static final HamonPath WARRIOR = register("warrior", "Warrior Path", "Jonathan Joestar",
            List.of(TurquoiseBlueOverdriveMove.ID,
                    ScarletOverdriveMove.ID));
    public static final HamonPath WISDOM = register("wisdom", "Wisdom Path", "Tonpetty",
            List.of(ForcedRippleBreathingMove.ID));
    public static final HamonPath DEFENDER = register("defender", "Defender Path", "Dire",
            List.of(ThunderCrossSplitAttackMove.ID,
                    RippleRebukeMove.ID));
    public static final HamonPath ENVIOUS = register("envious", "Envious Path", "Straizo",
            List.of(YouthfulBreathMove.ID));
    public static final HamonPath TRICKSTER = register("trickster", "Trickster Path", "Joseph Joestar",
            List.of(ClackerVolleyMove.ID,
                    BottleCapShotMove.ID,
                    NextLineMove.ID));
    public static final HamonPath PRIDEFUL = register("prideful", "Prideful Path", "Caesar Zeppeli",
            List.of(BubbleCutterMove.ID,
                    BubbleLauncherMove.ID));
    public static final HamonPath STYLISH = register("stylish", "Stylish Path", "Lisa Lisa",
            List.of(SnakeMufflerMove.ID,
                    RippleWalkMove.ID));

    private static final Map<ResourceLocation, HamonPath> MODES = new LinkedHashMap<>();

    public static final HamonPath WIND_MODE = mode("wind_mode", "Wind Mode", "Wamuu", List.of(
            DivineSandstormMove.ID,
            GaleCutMove.ID,
            TornadoMove.ID,
            WindCloakMove.ID,
            AtmosphericRiftMove.ID));
    public static final HamonPath HEAT_MODE = mode("heat_mode", "Heat Mode", "Esidisi", List.of(
            BoilingBloodNeedlesMove.ID,
            BloodBoilMove.ID,
            VesselGrabMove.ID,
            HeatBodyMove.ID,
            CryingVentMove.ID));
    public static final HamonPath LIGHT_MODE = mode("light_mode", "Light Mode", "Kars", List.of(
            BrilliantBladesMove.ID,
            ShiningSlashMove.ID,
            BladeFlashMove.ID,
            SawGripMove.ID));

    private HamonPaths() {
    }

    private static HamonPath mode(String path, String displayName, String teacher, List<ResourceLocation> moveIds) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        HamonPath mode = new HamonPath(id, displayName, teacher, moveIds);
        MODES.put(id, mode);
        return mode;
    }

    public static Collection<HamonPath> modes() {
        return MODES.values();
    }

    public static boolean isMode(HamonPath path) {
        return path != null && MODES.containsKey(path.id());
    }

    public static boolean isMode(ResourceLocation id) {
        return MODES.containsKey(id);
    }

    public static HamonPath randomMode(RandomSource random) {
        List<HamonPath> all = List.copyOf(MODES.values());
        return all.get(random.nextInt(all.size()));
    }

    private static HamonPath register(String path, String displayName, String teacher, List<ResourceLocation> moveIds) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        HamonPath hamonPath = new HamonPath(id, displayName, teacher, moveIds);
        PATHS.put(id, hamonPath);
        return hamonPath;
    }

    public static HamonPath byId(ResourceLocation id) {
        HamonPath path = PATHS.get(id);
        return path != null ? path : MODES.get(id);
    }

    public static Collection<HamonPath> all() {
        return PATHS.values();
    }

    public static HamonPath random(RandomSource random) {
        List<HamonPath> all = List.copyOf(PATHS.values());
        return all.get(random.nextInt(all.size()));
    }

    public static void bootstrap() {
    }
}
