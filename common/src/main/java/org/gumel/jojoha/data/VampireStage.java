package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;

/**
 * Progression stage within the Vampire chain: Human -> Vampire -> Pillar Man -> Ultimate Lifeform.
 * Only meaningful while {@link PlayerSpec#VAMPIRISM} is active. Stubbed for a future pass —
 * no gameplay behavior is wired up to this yet.
 */
public enum VampireStage {
    NONE,
    VAMPIRE,
    PILLAR_MAN,
    ULTIMATE_LIFEFORM;

    public static final Codec<VampireStage> CODEC = Codec.STRING.xmap(
            name -> VampireStage.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            stage -> stage.name().toLowerCase(java.util.Locale.ROOT)
    );
}
