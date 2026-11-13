package org.gumel.jojoha.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.stand.skill.StandReinforced;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.stand.skill.moves.StandDashSkill;
import org.gumel.jojoha.stand.skill.moves.TimeSkipSkill;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampireMoves;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillBook {
    public enum Category {
        PLAYER("presets/skills_player_preset.png"),
        HAMON("presets/skills_hamon_preset.png"),
        VAMPIRE("presets/skills_vampire_preset.png"),
        PILLAR("presets/skills_pillarmen_preset.png"),
        STAND("presets/skills_stand_preset.png");

        private final String preset;

        Category(String preset) {
            this.preset = preset;
        }

        public ResourceLocation preset() {
            return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/gui/menu/" + preset);
        }

        public String translationKey() {
            return "skills.jojoha." + name().toLowerCase(Locale.ROOT);
        }

        public String translationKey(JojohaPlayerData data) {
            return this == VAMPIRE && Fate.ultimate(data) ? "skills.jojoha.ultimate" : translationKey();
        }

        public String descriptionKey(JojohaPlayerData data) {
            return translationKey(data) + ".desc";
        }

        public String descriptionKey() {
            return translationKey() + ".desc";
        }

        public Category next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public boolean reachable(JojohaPlayerData data) {
            return switch (this) {
                case PLAYER -> true;
                case HAMON -> data.discipline == Discipline.HAMON;
                case VAMPIRE -> Fate.undead(data);
                case PILLAR -> Fate.pillar(data);
                case STAND -> data.stand.isPresent();
            };
        }

        public Category next(JojohaPlayerData data) {
            Category at = this;
            for (int i = 0; i < values().length; i++) {
                at = at.next();
                if (at.reachable(data)) {
                    return at;
                }
            }
            return PLAYER;
        }

        public Category settle(JojohaPlayerData data) {
            return reachable(data) ? this : PLAYER;
        }

        public Category previous() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }
    }

    public record Entry(ResourceLocation id, Component name, boolean unlocked, boolean dashForm) {
        public Entry(ResourceLocation id, Component name, boolean unlocked) {
            this(id, name, unlocked, false);
        }
    }

    private SkillBook() {
    }

    public static String emptyReason(Category category, JojohaPlayerData data) {
        if (!of(category, data).isEmpty()) {
            return null;
        }

        return switch (category) {
            case PLAYER -> "Learn a move on the Player tree";
            case HAMON -> data.getUnlockedHamonPaths().isEmpty()
                    ? "Learn a Hamon path first"
                    : "Learn a move on the Hamon tree";
            case VAMPIRE -> data.vampireStage == VampireStage.NONE
                    ? "Wear a Stone Mask to turn first"
                    : Fate.ultimate(data) ? "Learn a move on the Ultimate Lifeform tree"
                    : "Learn a move on the Vampirism tree";
            case PILLAR -> "Learn a move on the Pillar Man tree";
            case STAND -> null;
        };
    }

    public static List<Entry> of(Category category, JojohaPlayerData data) {
        List<Entry> entries = new ArrayList<>();

        switch (category) {
            case STAND -> {
                for (StandSkill skill : StandSkills.all()) {
                    if (StandSkills.inMoveset(data, skill)
                            && Moves.canEquip(data, skill.id())) {
                        entries.add(new Entry(skill.id(),
                                Component.translatable(skill.translationKeyFor(data)), true));
                    }
                }
                if (StandReinforced.dashChoice(data)) {
                    for (StandSkill form : List.of(StandDashSkill.INSTANCE,
                            TimeSkipSkill.INSTANCE)) {
                        entries.add(new Entry(form.id(),
                                Component.translatable(form.translationKeyFor(data)), true, true));
                    }
                }
            }
            case HAMON -> {
                for (HamonMove move : HamonMoves.all()) {
                    if (!Moves.canEquip(data, move.id())) {
                        continue;
                    }
                    entries.add(new Entry(move.id(), Component.translatable(
                            "move.jojoha." + move.id().getPath()), true));
                }
            }
            case VAMPIRE, PILLAR -> {
                for (VampireMove move
                        : VampireMoves.all()) {
                    if (move.pillar() != (category == Category.PILLAR)
                            || !Moves.canEquip(data, move.id())) {
                        continue;
                    }
                    entries.add(new Entry(move.id(), Component.translatable(
                            "move.jojoha." + move.id().getPath()), true));
                }
            }
            case PLAYER -> {
                for (StandSkill skill : StandSkills.all()) {
                    if (skill.requiresStand()
                            || !Moves.canEquip(data, skill.id())) {
                        continue;
                    }
                    entries.add(new Entry(skill.id(),
                            Component.translatable(skill.translationKeyFor(data)), true));
                }
            }
        }

        return entries;
    }
}
