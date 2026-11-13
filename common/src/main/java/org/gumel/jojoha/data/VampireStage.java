package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum VampireStage {
    NONE,
    VAMPIRE,
    PILLAR_MAN,
    ULTIMATE_LIFEFORM;

    public static final Codec<VampireStage> CODEC = Codec.STRING.xmap(
            name -> VampireStage.valueOf(name.toUpperCase(Locale.ROOT)),
            stage -> stage.name().toLowerCase(Locale.ROOT)
    );
}
