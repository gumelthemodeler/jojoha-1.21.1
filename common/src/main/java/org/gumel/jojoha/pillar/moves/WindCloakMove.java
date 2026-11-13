package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class WindCloakMove extends PillarMove {
    public static final ResourceLocation ID = id("wind_cloak");
    public static final WindCloakMove INSTANCE = new WindCloakMove();

    public static final float DRAIN_PER_TICK = 0.2F;

    private WindCloakMove() {
        super("wind_cloak", 10, 0F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        boolean on = PillarMen.flip(player, PillarMen.Toggle.CLOAK);
        MovementFx.pose(player, PillarPoses.WIND_CLOAK);
        StandBeat.at(PillarPoses.WIND_CLOAK_REVEAL_TICKS, player, () -> PillarFx.flare(player, PillarFx.WIND_STYLE, on));
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI * 2 / 10;
            PillarFx.fling(level, PillarFx.gust(), player.position().add(Math.cos(angle) * 0.8, 0.3 + i * 0.12,
                    Math.sin(angle) * 0.8), new Vec3(-Math.sin(angle) * 0.15, 0.02, Math.cos(angle) * 0.15));
        }
        level.playSound(null, player.blockPosition(), on ? SoundEvents.BREEZE_INHALE : SoundEvents.BREEZE_IDLE_AIR,
                SoundSource.PLAYERS, 0.8F, 1.0F);
    }
}
