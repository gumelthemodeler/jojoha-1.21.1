package org.gumel.jojoha.stand.mob;

import java.util.UUID;

public interface MobStandHolder {
    String jojoha$standId();

    int jojoha$standSkin();

    void jojoha$setStand(String standId, int skin);

    UUID jojoha$standEntity();

    void jojoha$setStandEntity(UUID standEntity);

    int jojoha$calmTicks();

    void jojoha$setCalmTicks(int ticks);
}
