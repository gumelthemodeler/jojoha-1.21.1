package org.gumel.jojoha.skill;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.pillar.PillarMen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class DisciplinePassives {
    private static final Set<String> MINOR = Set.of("might", "constitution", "fleetness", "hardiness",
            "resolve", "held_breath", "unbroken_fall", "undead_physiology", "sun_immunity", "perfect_biology");

    public record DisciplinePassive(ResourceLocation id, Discipline spec,
                              Predicate<JojohaPlayerData> active) {

        public String translationKey() {
            return "spec_passive." + id.getNamespace() + "." + id.getPath();
        }

        public String descriptionKey() {
            return translationKey() + ".desc";
        }

        public boolean minor() {
            return MINOR.contains(id.getPath());
        }
    }

    private static final Map<ResourceLocation, DisciplinePassive> PASSIVES = new LinkedHashMap<>();

    private DisciplinePassives() {
    }

    private static void register(String path, Discipline spec, Predicate<JojohaPlayerData> active) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        PASSIVES.put(id, new DisciplinePassive(id, spec, active));
    }

    private static boolean hermit(JojohaPlayerData data) {
        return data.hasHamonPath(HamonPaths.HERMIT.id());
    }

    private static boolean turned(JojohaPlayerData data) {
        return data.vampireStage != VampireStage.NONE;
    }

    private static boolean ultimate(JojohaPlayerData data) {
        return data.vampireStage == VampireStage.ULTIMATE_LIFEFORM;
    }

    private static boolean burns(JojohaPlayerData data) {
        return turned(data) && !ultimate(data);
    }

    static {
        register("might", Discipline.NONE, data -> true);
        register("constitution", Discipline.NONE, data -> true);
        register("fleetness", Discipline.NONE, data -> true);
        register("hardiness", Discipline.NONE, data -> true);
        register("resolve", Discipline.NONE, data -> true);

        register("ripple_breathing", Discipline.HAMON, data -> true);

        register("hermit_bloom", Discipline.HAMON, DisciplinePassives::hermit);
        register("held_breath", Discipline.HAMON, DisciplinePassives::hermit);

        register("undead_physiology", null, DisciplinePassives::turned);
        register("blood_mending", null, DisciplinePassives::turned);
        register("unbroken_fall", null, DisciplinePassives::turned);

        register("sun_scorch", null, DisciplinePassives::burns);

        register("flesh_absorption", null, Fate::pillar);
        register("stone_in_sunlight", null, data -> data.vampireStage == VampireStage.PILLAR_MAN);
        register("wind_sense", null, PillarMen::windMode);
        register("boiling_blood", null, PillarMen::heatMode);
        register("blade_guard", null, PillarMen::lightMode);

        register("perfect_biology", null, DisciplinePassives::ultimate);
        register("sun_immunity", null, DisciplinePassives::ultimate);
    }

    public static List<DisciplinePassive> of(Discipline spec) {
        List<DisciplinePassive> out = new ArrayList<>();
        for (DisciplinePassive passive : PASSIVES.values()) {
            if (passive.spec() == spec) {
                out.add(passive);
            }
        }
        return out;
    }

    public static List<DisciplinePassive> carried(JojohaPlayerData data) {
        List<DisciplinePassive> out = of(data.discipline);
        if (Fate.undead(data)) {
            out.addAll(of(null));
        }
        return out;
    }

    public static DisciplinePassive byId(ResourceLocation id) {
        return PASSIVES.get(id);
    }

    public static Collection<DisciplinePassive> all() {
        return PASSIVES.values();
    }
}
