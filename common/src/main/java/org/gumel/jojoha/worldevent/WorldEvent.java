package org.gumel.jojoha.worldevent;

import org.jetbrains.annotations.Nullable;

public enum WorldEvent {
    VAMPIRE_NIGHT("vampire_night"),
    METEOR_SHOWER("meteor_shower"),
    VAMPIRE_RAID("vampire_raid");

    private final String id;

    WorldEvent(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    @Nullable
    public static WorldEvent byId(String id) {
        for (WorldEvent event : values()) {
            if (event.id.equals(id)) {
                return event;
            }
        }
        return null;
    }
}
