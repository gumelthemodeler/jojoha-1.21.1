package org.gumel.jojoha.skilltree;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.gumel.jojoha.data.Discipline;
import org.gumel.jojoha.data.Fate;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.data.VampireStage;
import org.gumel.jojoha.hamon.HamonPath;
import org.gumel.jojoha.hamon.HamonPaths;
import org.gumel.jojoha.stand.skill.moves.TimeStopSkill;

import java.util.List;

public record SkillNode(ResourceLocation id, ResourceLocation skill, ResourceLocation parent,
                        Branch branch, int x, int y, List<StatRequirement> stats,
                        List<ItemRequirement> items, List<Gate> gates,
                        List<ResourceLocation> requires, boolean innate) {
    public enum Branch {
        CORE("Core", 0xFFF2E4B0),
        COMBAT("Combat", 0xFFE06A5A),

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

    public interface Gate {

        boolean met(JojohaPlayerData data);

        String describe(JojohaPlayerData data);

        static Gate hamonPath(HamonPath path) {
            return new Gate() {
                @Override
                public boolean met(JojohaPlayerData data) {
                    return data.hasHamonPath(path.id());
                }

                @Override
                public String describe(JojohaPlayerData data) {
                    return "Learn the " + path.displayName();
                }
            };
        }

        static Gate modeBook(HamonPath path) {
            return new Gate() {
                @Override
                public boolean met(JojohaPlayerData data) {
                    return data.hasHamonPath(path.id());
                }

                @Override
                public String describe(JojohaPlayerData data) {
                    return "Read the " + path.displayName() + " Book";
                }
            };
        }

        Gate HAMON_BOOK = new Gate() {
            @Override
            public boolean met(JojohaPlayerData data) {
                return data.discipline == Discipline.HAMON && data.hasHamonPath(HamonPaths.HERMIT.id());
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Read the " + HamonPaths.HERMIT.displayName() + " Book";
            }
        };

        Gate TURNED = new Gate() {
            @Override
            public boolean met(JojohaPlayerData data) {
                return data.vampireStage != VampireStage.NONE;
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Wear a Stone Mask to turn";
            }
        };

        Gate PILLAR_MAN = new Gate() {
            @Override
            public boolean met(JojohaPlayerData data) {
                return data.vampireStage.ordinal()
                        >= VampireStage.PILLAR_MAN.ordinal();
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Become a Pillar Man";
            }
        };

        Gate ULTIMATE = new Gate() {
            @Override
            public boolean met(JojohaPlayerData data) {
                return Fate.ultimate(data);
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Become the Ultimate Lifeform";
            }
        };

        Gate TIME_STOP_EXPOSURE = new Gate() {
            @Override
            public boolean met(JojohaPlayerData data) {
                return TimeStopSkill.hasLearned(data);
            }

            @Override
            public String describe(JojohaPlayerData data) {
                return "Witness Time Stop ("
                        + data.timeStopExposures + "/"
                        + TimeStopSkill.REQUIRED_EXPOSURES + ")";
            }
        };
    }

    public record StatRequirement(boolean stand, int stat, int minimum) {
        public boolean met(JojohaPlayerData data) {
            return current(data) >= minimum;
        }

        public int current(JojohaPlayerData data) {
            if (!stand) {
                return StatPoints.playerStat(data, stat);
            }
            return data.stand.isPresent() ? data.stand.stats()[stat] : 0;
        }

        public String abbreviation() {
            String[] names = stand ? StatPoints.STAND_ABBREVIATIONS : StatPoints.PLAYER_ABBREVIATIONS;
            return stat >= 0 && stat < names.length ? names[stat] : "???";
        }

        public String describe() {
            return minimum + " " + abbreviation();
        }
    }

    public record ItemRequirement(Item item, int count) {
        public boolean met(Player player) {
            return countHeld(player) >= count;
        }

        public int countHeld(Player player) {
            int found = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(item)) {
                    found += stack.getCount();
                }
            }
            return found;
        }

        public void consume(Player player) {
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

    public static SkillNode root(ResourceLocation id) {
        return new SkillNode(id, null, null, Branch.CORE, 0, 0,
                List.of(), List.of(), List.of(), List.of(), false);
    }

    public static SkillNode innate(ResourceLocation id, ResourceLocation skill,
                                   ResourceLocation parent, Branch branch, int x, int y) {
        return new SkillNode(id, skill, parent, branch, x, y,
                List.of(), List.of(), List.of(), List.of(), true);
    }

    public boolean isRoot() {
        return parent == null;
    }

    public String translationKey() {
        return "skilltree.jojoha." + id.getPath();
    }
}
