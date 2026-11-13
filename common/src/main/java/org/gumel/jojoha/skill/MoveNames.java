package org.gumel.jojoha.skill;

import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;

public final class MoveNames {

    private MoveNames() {
    }

    public static String key(JojohaPlayerData data, ResourceLocation id) {
        if (id == null) {
            return "";
        }

        StandSkill stand = StandSkills.byId(id);
        return stand != null ? stand.translationKeyFor(data) : "move.jojoha." + id.getPath();
    }

    public static String descriptionKey(JojohaPlayerData data, ResourceLocation id) {
        String name = key(data, id);
        return name.isEmpty() ? "" : name + ".desc";
    }

    public static boolean hasDescription(JojohaPlayerData data, ResourceLocation id) {
        String key = descriptionKey(data, id);
        return !key.isEmpty() && Language.getInstance().has(key);
    }
}
