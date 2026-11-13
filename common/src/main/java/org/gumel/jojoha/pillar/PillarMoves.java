package org.gumel.jojoha.pillar;

import org.gumel.jojoha.pillar.moves.AtmosphericRiftMove;
import org.gumel.jojoha.pillar.moves.BladeFlashMove;
import org.gumel.jojoha.pillar.moves.BoilingBloodNeedlesMove;
import org.gumel.jojoha.pillar.moves.BrilliantBladesMove;
import org.gumel.jojoha.pillar.moves.ContortionMove;
import org.gumel.jojoha.pillar.moves.CryingVentMove;
import org.gumel.jojoha.pillar.moves.DivineSandstormMove;
import org.gumel.jojoha.pillar.moves.GaleCutMove;
import org.gumel.jojoha.pillar.moves.HeatBodyMove;
import org.gumel.jojoha.pillar.moves.InfiltrateMove;
import org.gumel.jojoha.pillar.moves.RibBladesMove;
import org.gumel.jojoha.pillar.moves.SawGripMove;
import org.gumel.jojoha.pillar.moves.ShiningSlashMove;
import org.gumel.jojoha.pillar.moves.TornadoMove;
import org.gumel.jojoha.pillar.moves.VesselGrabMove;
import org.gumel.jojoha.pillar.moves.WindCloakMove;
import org.gumel.jojoha.vampire.VampireMove;

import java.util.List;

public final class PillarMoves {
    public static final List<VampireMove> ALL = List.of(
            InfiltrateMove.INSTANCE,
            ContortionMove.INSTANCE,
            RibBladesMove.INSTANCE,
            DivineSandstormMove.INSTANCE,
            GaleCutMove.INSTANCE,
            TornadoMove.INSTANCE,
            WindCloakMove.INSTANCE,
            AtmosphericRiftMove.INSTANCE,
            BoilingBloodNeedlesMove.INSTANCE,
            VesselGrabMove.INSTANCE,
            HeatBodyMove.INSTANCE,
            CryingVentMove.INSTANCE,
            BrilliantBladesMove.INSTANCE,
            ShiningSlashMove.INSTANCE,
            BladeFlashMove.INSTANCE,
            SawGripMove.INSTANCE);

    private PillarMoves() {
    }
}
