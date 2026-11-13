package org.gumel.jojoha.stand.workshop;

import org.gumel.jojoha.stand.StandType;
import org.gumel.jojoha.stand.StandTypes;

public enum RestPose {
    CALM("calm"),
    READY("ready");

    private final String key;

    RestPose(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public boolean fits(StandType type) {
        return this == CALM || type.form().hasBody() && !type.form().isBound()
                && !StandTypes.SILVER_CHARIOT_ID.equals(type.id()) && !StandTypes.MAGICIANS_RED_ID.equals(type.id());
    }

    public static RestPose of(int index) {
        RestPose[] all = values();
        return index >= 0 && index < all.length ? all[index] : CALM;
    }
}
