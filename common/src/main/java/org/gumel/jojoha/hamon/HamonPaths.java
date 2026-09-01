package org.gumel.jojoha.hamon;

import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.hamon.moves.RipplePulseMove;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The 8 named Hamon paths from the design doc. */
public final class HamonPaths {
    private static final Map<ResourceLocation, HamonPath> PATHS = new LinkedHashMap<>();

    public static final HamonPath HERMIT = register("hermit", "Hermit Path", null, List.of(RipplePulseMove.ID));
    public static final HamonPath WARRIOR = register("warrior", "Warrior Path", "Jonathan Joestar", List.of());
    public static final HamonPath WISDOM = register("wisdom", "Wisdom Path", "Tonpetty", List.of());
    public static final HamonPath DEFENDER = register("defender", "Defender Path", "Dire", List.of());
    public static final HamonPath ENVIOUS = register("envious", "Envious Path", "Straizo", List.of());
    public static final HamonPath TRICKSTER = register("trickster", "Trickster Path", "Joseph Joestar", List.of());
    public static final HamonPath PRIDEFUL = register("prideful", "Prideful Path", "Caesar Zeppeli", List.of());
    public static final HamonPath STYLISH = register("stylish", "Stylish Path", "Lisa Lisa", List.of());

    private HamonPaths() {
    }

    private static HamonPath register(String path, String displayName, String teacher, List<ResourceLocation> moveIds) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, path);
        HamonPath hamonPath = new HamonPath(id, displayName, teacher, moveIds);
        PATHS.put(id, hamonPath);
        return hamonPath;
    }

    public static HamonPath byId(ResourceLocation id) {
        return PATHS.get(id);
    }

    /** No-op call site to force this class's static initializers to run. */
    public static void bootstrap() {
    }
}
