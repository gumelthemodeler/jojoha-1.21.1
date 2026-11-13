package org.gumel.jojoha.skill;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.Burning;
import org.gumel.jojoha.combat.PlayerStun;
import org.gumel.jojoha.config.JojohaServerConfig;
import org.gumel.jojoha.data.StatPerk;

import java.util.List;
import java.util.Map;

public final class Inflicts {
    public record Status(String effect, int level, int colour) {
        public Component name() {
            MutableComponent name = Component.translatable("effect.jojoha." + effect);
            return level > 0 ? name.append(" ").append(Component.translatable("enchantment.level." + level)) : name;
        }

        public Component definition() {
            String key = "keyword.jojoha." + effect + (level > 0 ? "." + level : "");
            return switch (effect) {
                case "bleed" -> Component.translatable(key, figure(Bleed.toll(0)),
                        figure(Bleed.toll(1)), figure(Bleed.toll(2)));
                case "burning" -> Component.translatable(key, figure(Burning.perSecond(0)),
                        figure(Burning.perSecond(1)),
                        figure(Burning.perSecond(2)),
                        figure(JojohaServerConfig.BURN_TICKS.whole() / 20F));
                case "guard_break" -> Component.translatable(key);
                default -> Component.translatable(key, figure(PlayerStun.MAX_TICKS / 20F));
            };
        }

        public int pips() {
            return Math.max(1, level);
        }

        public Indicator indicator() {
            return switch (effect) {
                case "bleed" -> Indicator.BLEED;
                case "stun" -> level >= 2 ? Indicator.STUN_II : Indicator.STUN;
                case "burning" -> Indicator.BURNING;
                case "guard_break" -> Indicator.GUARD_BREAK;
                default -> null;
            };
        }

        public Component tagged() {
            return tagged(colour);
        }

        public Component tagged(int shown) {
            Indicator indicator = indicator();
            MutableComponent named = name().copy().withStyle(style -> style.withColor(shown));
            return indicator != null ? indicator.line(named) : Component.empty().append(tag(colour)).append(named);
        }
    }

    public static final ResourceLocation TAG_FONT = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tag");

    public static MutableComponent tag(int colour) {
        return Component.literal("\uE700")
                .withStyle(style -> style.withFont(TAG_FONT).withColor(colour & 0xFFFFFF));
    }

    public static final Status BLEEDING = new Status("bleed", 0, 0xD02828);
    public static final Status STUN_ONE = new Status("stun", 1, 0xF0D040);
    public static final Status STUN_TWO = new Status("stun", 2, 0xF0D040);
    public static final Status GUARD_BROKEN = new Status("guard_break", 0, 0x8FB8E8);
    public static final Status BURNING = new Status("burning", 0, 0xF08020);

    public static final List<Status> ALL = List.of(BLEEDING, STUN_ONE, STUN_TWO, BURNING, GUARD_BROKEN);

    public static final Map<String, List<Status>> BY_MOVE = Map.ofEntries(
            Map.entry("gut_punch", List.of(BLEEDING)),
            Map.entry("beatdown", List.of(BLEEDING)),
            Map.entry("vampiric_drive", List.of(BLEEDING)),
            Map.entry("knife_barrage", List.of(BLEEDING)),
            Map.entry("blade_launch", List.of(BLEEDING)),
            Map.entry("sabre_cycle", List.of(BLEEDING)),
            Map.entry("space_ripper_stingy_eyes", List.of(BLEEDING)),
            Map.entry("draw", List.of(BLEEDING)),
            Map.entry("emerald_splash_shotgun_blast", List.of(BLEEDING)),
            Map.entry("uppercut", List.of(STUN_ONE)),
            Map.entry("star_finger", List.of(STUN_ONE)),
            Map.entry("ground_breaker", List.of(STUN_ONE)),
            Map.entry("tendril_spike", List.of(STUN_ONE)),
            Map.entry("thunder_cross_split_attack", List.of(STUN_ONE)),
            Map.entry("riposte", List.of(STUN_ONE)),
            Map.entry("twisting_gut_punch", List.of(STUN_TWO)),
            Map.entry("skull_crusher", List.of(STUN_TWO)),
            Map.entry("thorn_whip", List.of(STUN_TWO)),
            Map.entry("balestra", List.of(STUN_TWO)),
            Map.entry("miniature_trap", List.of(STUN_TWO)),
            Map.entry("unravel_trap", List.of(STUN_TWO)),
            Map.entry("sunlight_yellow_overdrive", List.of(STUN_TWO)),
            Map.entry("muda_rush", List.of(GUARD_BROKEN)),
            Map.entry("heavens_uppercut", List.of(STUN_ONE, GUARD_BROKEN)),
            Map.entry("judgement_fist", List.of(STUN_ONE, GUARD_BROKEN)),
            Map.entry("heaven_step", List.of(STUN_ONE, GUARD_BROKEN)),
            Map.entry("knife_halo", List.of(BLEEDING)),
            Map.entry("erasure", List.of(STUN_TWO)),
            Map.entry("scarlet_overdrive", List.of(BURNING)),
            Map.entry("controlled_blaze", List.of(BURNING)),
            Map.entry("sweep_kick", List.of(STUN_ONE)),
            Map.entry("crossfire_hurricane", List.of(BURNING)),
            Map.entry("crossfire_hurricane_special", List.of(BURNING)),
            Map.entry("red_bind", List.of(STUN_TWO, BURNING)),
            Map.entry("inferno", List.of(BURNING)),
            Map.entry("containment", List.of(BURNING)),
            Map.entry("infinite_inferno_fire", List.of(BURNING)));

    public static final Map<StatPerk, List<Status>> BY_PERK = Map.of(
            StatPerk.SHIELDBREAKER, List.of(GUARD_BROKEN),
            StatPerk.STAGGERING_BLOW, List.of(STUN_ONE),
            StatPerk.GUARD_CRUSHER, List.of(GUARD_BROKEN));

    private Inflicts() {
    }

    public static List<Status> of(ResourceLocation move) {
        return move == null ? List.of() : BY_MOVE.getOrDefault(move.getPath(), List.of());
    }

    public static List<Status> of(StatPerk perk) {
        return perk == null ? List.of() : BY_PERK.getOrDefault(perk, List.of());
    }

    public static Component line(ResourceLocation move) {
        return line(of(move));
    }

    public static Component line(List<Status> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        MutableComponent names = Component.empty();
        for (int i = 0; i < statuses.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            Status status = statuses.get(i);
            names.append(status.tagged());
        }
        return Component.translatable("tooltip.jojoha.inflicts", names).withStyle(ChatFormatting.GRAY);
    }

    private static String figure(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value))
                : String.valueOf(Math.round(value * 10F) / 10F);
    }
}
