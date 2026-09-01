package org.gumel.jojoha.client;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Everything a player could learn, sorted into the four groups the interface shows.
 *
 * <p>A view, not a registry. The moves themselves live where they always have - {@link StandSkills}
 * and {@link HamonMoves} - and this reads them into one shape the skill page can lay out. Building a
 * second list of them here would be a second place to forget to add a move to.
 *
 * <p>What it adds on top is the two things a registry has no opinion about: which group a move
 * belongs in, and whether this particular player has it yet.
 */
public final class SkillBook {
    /** The four groups, in the order the arrows walk through them. */
    public enum Category {
        PLAYER("skills_player_preset.png"),
        HAMON("skills_hamon_preset.png"),
        VAMPIRE("skills_vampire_preset.png"),
        STAND("skills_stand_preset.png");

        private final String preset;

        Category(String preset) {
            this.preset = preset;
        }

        /** The disc that sits in the round opening at the top of the frame. */
        public ResourceLocation preset() {
            return ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID,
                    "textures/gui/menu/" + preset);
        }

        public String translationKey() {
            return "skills.jojoha." + name().toLowerCase(Locale.ROOT);
        }

        /** What the group is for, shown on hovering the disc now the page has no room to say it. */
        public String descriptionKey() {
            return translationKey() + ".desc";
        }

        public Category next() {
            return values()[(ordinal() + 1) % values().length];
        }

        public Category previous() {
            return values()[(ordinal() + values().length - 1) % values().length];
        }
    }

    /**
     * One move as the page needs it.
     *
     * @param name     what to call it
     * @param unlocked whether this player has it, which decides whether it is drawn lit or dark
     */
    public record Entry(ResourceLocation id, Component name, boolean unlocked) {
    }

    private SkillBook() {
    }

    /**
     * What belongs on a category's page for this player.
     *
     * <p>Only what this player actually has. Locked moves used to be listed and dimmed, on the
     * argument that a page showing only what you own tells you nothing about what there is to get -
     * which was true when this was the only place moves appeared. The skill tree answers that
     * question now, and answers it better, so the list can go back to being a list of your moves.
     */
    public static List<Entry> of(Category category, JojohaPlayerData data) {
        List<Entry> entries = new ArrayList<>();

        switch (category) {
            case STAND -> {
                for (StandSkill skill : StandSkills.all()) {
                    // The same test the server will apply when the click arrives, so nothing on the
                    // page can look available and then refuse. A move outside this Stand's moveset
                    // fails it too, which is right - it is not available, for this Stand.
                    if (StandSkills.canEquip(data, skill)) {
                        entries.add(new Entry(skill.id(),
                                Component.translatable(skill.translationKeyFor(data)), true));
                    }
                }
            }
            case HAMON -> {
                for (HamonMove move : HamonMoves.all()) {
                    // Hamon moves carry no translation key of their own yet, so the name is built
                    // from the id the same way the commands display one. When they grow one this is
                    // the only line that has to know.
                    entries.add(new Entry(move.id(), Component.translatable(
                            "move.jojoha." + move.id().getPath()), true));
                }
            }
            // Nothing is registered for either of these yet. They are listed anyway, because an
            // empty page that exists is a promise, and a missing one is a gap.
            case PLAYER, VAMPIRE -> {
            }
        }

        return entries;
    }
}
