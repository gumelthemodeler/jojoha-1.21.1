package org.gumel.jojoha.supporter;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class SupporterAuras {
    public static final int NONE = -1;
    public static final int RAINBOW = 0x1000000;
    public static final Map<String, Integer> PRESETS = presets();

    private static final int RAINBOW_TICKS = 120;
    private static final Pattern HEX = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final Map<UUID, Integer> AURAS = new ConcurrentHashMap<>();
    private static volatile int ownAura = NONE;
    private static volatile boolean ownSkins;
    private static volatile UUID ownId;

    private SupporterAuras() {
    }

    private static Map<String, Integer> presets() {
        Map<String, Integer> presets = new LinkedHashMap<>();
        presets.put("rainbow", RAINBOW);
        presets.put("red", 0xE0303A);
        presets.put("orange", 0xFF8A1E);
        presets.put("gold", 0xFFC832);
        presets.put("yellow", 0xFFF04A);
        presets.put("lime", 0x8CF03C);
        presets.put("green", 0x32C850);
        presets.put("cyan", 0x3CE6E6);
        presets.put("sky", 0x6EB4FF);
        presets.put("blue", 0x3A5AF0);
        presets.put("purple", 0x9A46E6);
        presets.put("pink", 0xFF78C8);
        presets.put("white", 0xF4F4F4);
        presets.put("shadow", 0x2A2A36);
        return Map.copyOf(presets);
    }

    public static boolean valid(int aura) {
        return aura == NONE || aura == RAINBOW || (aura >= 0 && aura <= 0xFFFFFF);
    }

    public static Integer parse(String raw) {
        String text = raw.trim().toLowerCase(Locale.ROOT);
        Integer preset = PRESETS.get(text);
        if (preset != null) {
            return preset;
        }
        return HEX.matcher(text).matches() ? Integer.parseInt(text.replace("#", ""), 16) : null;
    }

    public static MutableComponent describe(int aura) {
        if (aura == RAINBOW) {
            MutableComponent word = Component.empty();
            String text = "Rainbow";
            for (int i = 0; i < text.length(); i++) {
                int colour = Mth.hsvToRgb(i / (float) text.length(), 0.7f, 1f);
                word.append(Component.literal(String.valueOf(text.charAt(i)))
                        .withStyle(style -> style.withColor(colour)));
            }
            return word;
        }
        String name = PRESETS.entrySet().stream().filter(entry -> entry.getValue() == aura)
                .map(Map.Entry::getKey).findFirst()
                .map(key -> Character.toUpperCase(key.charAt(0)) + key.substring(1))
                .orElse(String.format("#%06X", aura & 0xFFFFFF));
        return Component.literal(name).withStyle(style -> style.withColor(aura & 0xFFFFFF));
    }

    public static void received(UUID player, int aura, boolean skins, boolean self) {
        if (!valid(aura) || aura == NONE) {
            AURAS.remove(player);
        } else {
            AURAS.put(player, aura);
        }
        if (self) {
            ownAura = valid(aura) ? aura : NONE;
            ownSkins = skins;
            ownId = player;
        }
    }

    private static int resolve(int aura, UUID owner) {
        if (aura != RAINBOW) {
            return aura;
        }
        long offset = owner == null ? 0 : Math.floorMod(owner.hashCode(), RAINBOW_TICKS);
        long tick = Util.getMillis() / 50L + offset;
        return Mth.hsvToRgb((tick % RAINBOW_TICKS) / (float) RAINBOW_TICKS, 0.7f, 1f) & 0xFFFFFF;
    }

    public static int tint(UUID owner, int base) {
        if (owner == null) {
            return base;
        }
        Integer aura = AURAS.get(owner);
        if (aura == null) {
            int shown = SupporterLink.auraOf(owner);
            return valid(shown) && shown != NONE ? resolve(shown, owner) : base;
        }
        return resolve(aura, owner);
    }

    public static int tint(Optional<UUID> owner, int base) {
        return tint(owner.orElse(null), base);
    }

    public static boolean chosen(UUID owner) {
        return owner != null && (AURAS.containsKey(owner) || owner.equals(ownId) && ownAura != NONE);
    }

    public static int ownTint(int base) {
        int aura = ownAura;
        return aura == NONE ? base : resolve(aura, ownId);
    }

    public static boolean ownSkins() {
        return ownSkins;
    }

    public static void clear() {
        AURAS.clear();
        ownAura = NONE;
        ownSkins = false;
        ownId = null;
    }
}
