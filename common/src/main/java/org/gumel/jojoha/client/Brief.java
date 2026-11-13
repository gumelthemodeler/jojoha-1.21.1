package org.gumel.jojoha.client;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.hamon.HamonMoves;
import org.gumel.jojoha.skill.Indicator;
import org.gumel.jojoha.skill.Inflicts;
import org.gumel.jojoha.skill.MoveNames;
import org.gumel.jojoha.skill.Moves;
import org.gumel.jojoha.stand.skill.StandSkill;
import org.gumel.jojoha.stand.skill.StandSkills;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampireMoves;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Brief {
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9+])");
    private static final Pattern PRICE = Pattern.compile(
            "^(?:Costs? \\d+(?:\\.\\d+)?(?: [A-Za-z]+)?|\\d+(?:\\.\\d+)?s(?:econds?)? cooldown)\\.?$");

    private Brief() {
    }

    public static boolean has(String nameKey) {
        return nameKey != null && !nameKey.isEmpty() && Language.getInstance().has(nameKey + ".short");
    }

    public static Component summary(String nameKey, String descKey, int body, int figure) {
        if (has(nameKey)) {
            return Component.translatable(nameKey + ".short").withStyle(style -> style.withColor(body));
        }
        return Figures.highlight(Component.translatable(descKey), body, figure);
    }

    public static boolean add(List<Component> lines, String nameKey, String descKey, boolean detailed,
                              boolean priced, int body, int figure) {
        boolean described = descKey != null && Language.getInstance().has(descKey);
        if (!has(nameKey)) {
            if (described) {
                lines.add(Figures.highlight(Component.translatable(descKey), body, figure));
            }
            return false;
        }

        lines.add(Component.translatable(nameKey + ".short").withStyle(style -> style.withColor(body)));
        if (!described) {
            return false;
        }

        List<String> rules = rules(Component.translatable(descKey).getString(), priced);
        if (!detailed) {
            return !rules.isEmpty();
        }
        for (String rule : rules) {
            lines.add(Figures.highlight(Component.literal("- " + rule), body, figure));
        }
        return false;
    }

    public static boolean move(List<Component> lines, JojohaPlayerData data, ResourceLocation move,
                               boolean detailed, int body, int figure) {
        String nameKey = MoveNames.key(data, move);
        if (nameKey.isEmpty()) {
            return false;
        }
        int at = lines.size();
        boolean more = add(lines, nameKey, nameKey + ".desc", detailed, true, body, figure);
        if (has(nameKey)) {
            lines.addAll(Math.min(at + 1, lines.size()), chips(data, move, nameKey, figure));
        } else {
            Component inflicts = Inflicts.line(move);
            if (inflicts != null) {
                lines.add(inflicts);
            }
        }
        return more;
    }

    static List<String> rules(String description, boolean priced) {
        List<String> rules = new ArrayList<>();
        for (String sentence : SENTENCE.split(description.trim())) {
            String rule = sentence.trim();
            if (rule.isEmpty() || priced && PRICE.matcher(rule).matches()) {
                continue;
            }
            rules.add(rule.endsWith(".") ? rule.substring(0, rule.length() - 1) : rule);
        }
        return rules;
    }

    public static List<Component> chips(JojohaPlayerData data, ResourceLocation move, String nameKey, int figure) {
        List<Component> chips = new ArrayList<>();

        float cost = cost(data, move);
        if (cost > 0F) {
            Indicator resource = HamonMoves.byId(move) != null ? Indicator.HAMON : Indicator.ABILITY;
            chips.add(resource.line(Component.translatable("tooltip.jojoha.chip.cost", number(cost))
                    .withStyle(style -> style.withColor(figure))));
        }
        int cooldown = Moves.scaledCooldown(data, move);
        if (cooldown > 0) {
            chips.add(Indicator.ABILITY.line(Component.translatable("tooltip.jojoha.chip.cooldown",
                    number(cooldown / 20F)).withStyle(style -> style.withColor(figure))));
        }
        if (Language.getInstance().has(nameKey + ".chip")) {
            chips.add(Indicator.ABILITY.line(Component.translatable("tooltip.jojoha.chip.range",
                    Component.translatable(nameKey + ".chip")).withStyle(style -> style.withColor(figure))));
        }
        for (Inflicts.Status status : Inflicts.of(move)) {
            chips.add(status.tagged());
        }
        return chips;
    }

    private static float cost(JojohaPlayerData data, ResourceLocation move) {
        StandSkill stand = StandSkills.byId(move);
        if (stand != null) {
            return stand.energyCost();
        }
        HamonMove hamon = HamonMoves.byId(move);
        if (hamon != null) {
            return hamon.energyCost();
        }
        VampireMove vampire = VampireMoves.byId(move);
        return vampire == null ? 0F : vampire.energyCost(data);
    }

    private static String number(float value) {
        if (value == Math.round(value)) {
            return String.valueOf(Math.round(value));
        }
        String text = String.format(Locale.ROOT, "%.2f", value);
        return text.endsWith("0") ? text.substring(0, text.length() - 1) : text;
    }
}
