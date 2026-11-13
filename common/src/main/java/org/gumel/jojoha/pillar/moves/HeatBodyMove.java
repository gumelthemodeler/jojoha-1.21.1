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

public final class HeatBodyMove extends PillarMove {
    public static final ResourceLocation ID = id("heat_body");
    public static final HeatBodyMove INSTANCE = new HeatBodyMove();

    public static final float DRAIN_PER_TICK = 0.22F;

    private HeatBodyMove() {
        super("heat_body", 10, 0F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        boolean on = PillarMen.flip(player, PillarMen.Toggle.HEAT);
        MovementFx.pose(player, PillarPoses.HEAT_BODY);
        StandBeat.at(PillarPoses.HEAT_BODY_REVEAL_TICKS, player, () -> PillarFx.flare(player, PillarFx.HEAT_STYLE, on));
        ServerLevel level = player.serverLevel();
        PillarFx.spray(level, PillarFx.steam(), player.position().add(0, 1.0, 0), on ? 12 : 6, 0.35, 0.04);
        if (on) {
            PillarFx.spray(level, PillarFx.bubble(), player.position().add(0, 1.0, 0), 8, 0.3, 0.05);
        }
        level.playSound(null, player.blockPosition(), on ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH,
                SoundSource.PLAYERS, 0.7F, on ? 0.7F : 1.2F);
    }
}
