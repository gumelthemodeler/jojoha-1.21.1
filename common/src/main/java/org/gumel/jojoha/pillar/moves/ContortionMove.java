package org.gumel.jojoha.pillar.moves;

import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.pillar.PillarMove;

public final class ContortionMove extends PillarMove {
    public static final ResourceLocation ID = id("contortion");
    public static final ContortionMove INSTANCE = new ContortionMove();

    public static final float DRAIN_PER_TICK = 0.12F;
    public static final double CRAWL_BONUS = 0.55;
    public static final double SPEED_BONUS = 0.25;
    public static final float SQUEEZE_WIDTH = 0.6F;
    public static final float SQUEEZE_HEIGHT = 0.45F;
    public static final float SQUEEZE_EYES = 0.3F;

    private ContortionMove() {
        super("contortion", 10, 0F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        boolean on = PillarMen.flip(player, PillarMen.Toggle.CONTORT);
        ServerLevel level = player.serverLevel();
        NetworkHandler.sendPillarFx(level, PillarFxKind.CONTORT_CRACK, player.getId(), 0, on ? 1 : 0);
    }
}
