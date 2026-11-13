package org.gumel.jojoha.stand.skill.moves;

import org.gumel.jojoha.stand.BarrageStruggle;
import org.gumel.jojoha.stand.StandClash;
import org.gumel.jojoha.stand.chariot.Chariot;
import org.gumel.jojoha.stand.magician.Magician;
import org.gumel.jojoha.vampire.moves.AmalgamateMove;

public final class MoveTickers {
    private MoveTickers() {
    }

    public static void init() {
        SkullCrusherSkill.init();
        BeatdownSkill.init();
        GroundBreakerSkill.init();
        PlayerFlurry.init();
        StandClash.init();
        BarrageStruggle.init();
        InhaleChannel.init();
        Chariot.init();
        Magician.init();
        AmalgamateMove.init();
    }
}
