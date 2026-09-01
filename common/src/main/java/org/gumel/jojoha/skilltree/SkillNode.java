package org.gumel.jojoha.skilltree;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatPoints;

import java.util.List;

/**
 * One node on a skill tree: a thing you can unlock, and what it costs to unlock it.
 *
 * <p>A node is not a move. Most nodes grant one - {@link #skill()} names it - but the node at the
 * centre of every tree grants nothing and only opens the path, and there is no reason a later node
 * could not grant a passive or a stat instead. Keeping the two separate is what lets the same tree
 * describe "Unlock Stand" and "Star Finger" without special cases.
 *
 * @param id       this node's own id, which is what gets written to the player's save
 * @param skill    the move this node grants, or null for a node that only opens a path
 * @param parent   the node that must be unlocked first, or null for a root
 * @param branch   which arm of the tree it belongs to, which decides how it is drawn
 * @param x        position in tree space, in pixels, with the root at the origin
 * @param y        the same
 * @param stats    stat minimums that must all be met
 * @param items    items that must be held, and which are consumed on unlocking
 * @param gates    conditions that are neither stats nor items - see {@link Gate}
 * @param requires other nodes that must also be taken, beyond the one this hangs from
 */
public record SkillNode(ResourceLocation id, ResourceLocation skill, ResourceLocation parent,
                        Branch branch, int x, int y, List<StatRequirement> stats,
                        List<ItemRequirement> items, List<Gate> gates,
                        List<ResourceLocation> requires) {

    /**
     * Which arm of the tree a node sits on.
     *
     * <p>Not decoration. The branch decides which way out of the hub a node is laid, what colour its
     * links are drawn, and what the tooltip calls it - so a tree that has grown to thirty nodes can
     * still be read at a glance as "the fighting side" and "the useful side".
     */
    public enum Branch {
        CORE("Core", 0xFFF2E4B0),
        COMBAT("Combat", 0xFFE06A5A),

        // Getting about: dashes, leaps, the short blinks. It used to be filed under utility, which
        // put a movement ability in the same group as the block placers on the grounds that neither
        // of them hits anything. That is a category built from what these moves are not. Anyone
        // looking for a dash is looking for a way to move, and now that is where it is.
        MOBILITY("Mobility", 0xFF7FD98F),
        UTILITY("Utility", 0xFF6ABEE0),
        SPECIAL("Special", 0xFFC08FFF);

        private final String label;
        private final int colour;

        Branch(String label, int colour) {
            this.label = label;
            this.colour = colour;
        }

        public String label() {
            return label;
        }

        public int colour() {
            return colour;
        }
    }

    /**
     * A condition that is neither a stat nor an item.
     *
     * <p>Kept as a named list rather than a free-form predicate so a node stays plain data - it has
     * to survive being read by the client, which cannot run the server's rules, only describe them.
     */
    public enum Gate {
        /**
         * Time Stop has to be witnessed before it can be learned, which is how the mod already
         * taught it before the tree existed. Putting the condition on the node rather than only on
         * the move means the tree shows you why it is shut instead of letting you buy something
         * that then refuses to work.
         */
        TIME_STOP_EXPOSURE {
            @Override
            public boolean met(JojohaPlayerData data) {
                return org.gumel.jojoha.stand.skill.moves.TimeStopSkill.hasLearned(data);
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Witness Time Stop ("
                        + data.timeStopExposures + "/"
                        + org.gumel.jojoha.stand.skill.moves.TimeStopSkill.REQUIRED_EXPOSURES + ")";
            }
        };

        public abstract boolean met(JojohaPlayerData data);

        public abstract String describe(JojohaPlayerData data);
    }

    /**
     * A minimum on one stat.
     *
     * @param stand   true to read the Stand's five rather than the player's
     * @param stat    index into the five, in the order the menu lists them
     * @param minimum what that stat has to reach
     */
    public record StatRequirement(boolean stand, int stat, int minimum) {
        /** Three letters each, in the same order as the name lists they shorten. */
        private static final String[] PLAYER_SHORT = {"STR", "VIT", "AGL", "END", "WOR"};
        private static final String[] STAND_SHORT = {"PWR", "SPD", "END", "PRT", "POT"};

        public boolean met(JojohaPlayerData data) {
            return current(data) >= minimum;
        }

        public int current(JojohaPlayerData data) {
            if (!stand) {
                return StatPoints.playerStat(data, stat);
            }
            return data.stand.isPresent() ? data.stand.stats()[stat] : 0;
        }

        /** The short form the tooltip uses, as in "16 PWR". */
        public String abbreviation() {
            String[] names = stand ? STAND_SHORT : PLAYER_SHORT;
            return stat >= 0 && stat < names.length ? names[stat] : "???";
        }

        /** The whole requirement in the form the tooltip lists it. */
        public String describe() {
            return minimum + " " + abbreviation();
        }
    }

    /**
     * An item that has to be held, and is taken when the node is unlocked.
     *
     * <p>Nothing on the Stand tree uses these - it is gated on stats alone - but Hamon paths are
     * meant to be bought with materials, so the cost is described here rather than bolted on later.
     */
    public record ItemRequirement(Item item, int count) {
        public boolean met(net.minecraft.world.entity.player.Player player) {
            return countHeld(player) >= count;
        }

        public int countHeld(net.minecraft.world.entity.player.Player player) {
            int found = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(item)) {
                    found += stack.getCount();
                }
            }
            return found;
        }

        /** Takes the items. Only ever called once the requirement is known to be met. */
        public void consume(net.minecraft.world.entity.player.Player player) {
            int left = count;
            for (ItemStack stack : player.getInventory().items) {
                if (left <= 0) {
                    return;
                }
                if (stack.is(item)) {
                    int taken = Math.min(left, stack.getCount());
                    stack.shrink(taken);
                    left -= taken;
                }
            }
        }

        public Component describe() {
            return Component.translatable(item.getDescriptionId());
        }
    }

    /** A node that opens a path without granting a move - the one at the centre of every tree. */
    public static SkillNode root(ResourceLocation id) {
        return new SkillNode(id, null, null, Branch.CORE, 0, 0,
                List.of(), List.of(), List.of(), List.of());
    }

    public boolean isRoot() {
        return parent == null;
    }

    /** The key for this node's own name, which for a move-granting node is the move's. */
    public String translationKey() {
        return "skilltree.jojoha." + id.getPath();
    }
}
