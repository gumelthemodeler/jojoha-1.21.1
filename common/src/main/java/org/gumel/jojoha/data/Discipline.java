package org.gumel.jojoha.data;

import com.mojang.serialization.Codec;

import java.util.Locale;

public enum Discipline {
    NONE,
    HAMON,
    ANUBIS;

    public static final Codec<Discipline> CODEC = Codec.STRING.xmap(
            Discipline::named,
            discipline -> discipline.name().toLowerCase(Locale.ROOT)
    );

    public static Discipline named(String name) {
        for (Discipline discipline : values()) {
            if (discipline.name().equalsIgnoreCase(name)) {
                return discipline;
            }
        }
        return NONE;
    }
}
