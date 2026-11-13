package org.gumel.jojoha.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.IdentityHashMap;
import java.util.Map;

public final class RawKey {
    private static final Map<KeyMapping, Boolean> WAS_DOWN = new IdentityHashMap<>();

    private static KeyMapping modifier;

    public static final String MODIFIER_KEY = "key.jojoha.skill_modifier";

    private RawKey() {
    }

    public static boolean isDown(KeyMapping mapping) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.screen != null || minecraft.player == null) {
            return false;
        }

        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        if (key.getType() != InputConstants.Type.KEYSYM
                || key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return mapping.isDown();
        }

        return InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue());
    }

    public static void setModifier(KeyMapping mapping) {
        modifier = mapping;
    }

    public static boolean modifierDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return false;
        }

        if (modifier != null && modifier.isUnbound()) {
            return false;
        }
        String bound = modifier == null ? "key.keyboard.left.alt" : modifier.saveString();
        if (!bound.equals("key.keyboard.left.alt") && !bound.equals("key.keyboard.right.alt")) {
            return isDown(modifier);
        }
        long window = minecraft.getWindow().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LALT)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RALT);
    }

    public static Component modifierName() {
        return modifier == null ? Component.translatable("key.keyboard.left.alt") : modifier.getTranslatedKeyMessage();
    }

    public static boolean pressed(KeyMapping mapping) {
        boolean down = isDown(mapping);
        boolean was = Boolean.TRUE.equals(WAS_DOWN.put(mapping, down));
        return down && !was;
    }
}
