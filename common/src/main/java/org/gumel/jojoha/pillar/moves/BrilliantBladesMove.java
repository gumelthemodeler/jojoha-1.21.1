package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class BrilliantBladesMove extends PillarMove {
    public static final ResourceLocation ID = id("brilliant_blades");
    public static final BrilliantBladesMove INSTANCE = new BrilliantBladesMove();

    public static final float DRAIN_PER_TICK = 0.18F;

    private BrilliantBladesMove() {
        super("brilliant_blades", 10, 0F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        boolean on = PillarMen.flip(player, PillarMen.Toggle.BLADES);
        MovementFx.pose(player, PillarPoses.BRILLIANT_BLADES);
        StandBeat.at(PillarPoses.BRILLIANT_BLADES_REVEAL_TICKS, player, () -> PillarFx.flare(player, PillarFx.LIGHT_STYLE, on));
        ServerLevel level = player.serverLevel();
        PillarFx.spray(level, PillarFx.glint(), player.position().add(0, 1.0, 0), 14, 0.4, 0.15);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 0.9F,
                on ? 1.6F : 0.8F);
    }
}
