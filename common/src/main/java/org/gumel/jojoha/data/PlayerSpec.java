package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;

/**
 * The combat spec a player currently has active. Per the design doc, a player holds
 * exactly one spec at a time; Stands are a separate, later-game unlock layered on top.
 */
public enum PlayerSpec {
    NONE,
    HAMON,
    VAMPIRISM;

    public static final Codec<PlayerSpec> CODEC = Codec.STRING.xmap(
            name -> PlayerSpec.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            spec -> spec.name().toLowerCase(java.util.Locale.ROOT)
    );
}
