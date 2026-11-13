package org.gumel.jojoha.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Figures {
    private static final Pattern FIGURE = Pattern.compile(
            "\\d+(?:\\.\\d+)?\\s*(?:%|x)?(?:\\s*(?:blocks?|seconds?|hearts?|damage|ticks?))?",
            Pattern.CASE_INSENSITIVE);

    private Figures() {
    }

    public static Component highlight(Component text, int body, int figure) {
        String plain = text.getString();
        MutableComponent out = Component.empty();

        Matcher matcher = FIGURE.matcher(plain);
        int cut = 0;
        while (matcher.find()) {
            if (matcher.start() > cut) {
                out.append(Component.literal(plain.substring(cut, matcher.start()))
                        .withStyle(style -> style.withColor(body)));
            }
            out.append(Component.literal(matcher.group())
                    .withStyle(style -> style.withColor(figure)));
            cut = matcher.end();
        }

        if (cut < plain.length()) {
            out.append(Component.literal(plain.substring(cut))
                    .withStyle(style -> style.withColor(body)));
        }
        return out;
    }
}
