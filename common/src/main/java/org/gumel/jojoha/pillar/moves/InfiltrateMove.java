package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMen;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.stand.hierophant.Possession;
import org.gumel.jojoha.stand.skill.StandBeat;

public final class InfiltrateMove extends PillarMove {
    public static final ResourceLocation ID = id("infiltrate");
    public static final InfiltrateMove INSTANCE = new InfiltrateMove();

    public static final double REACH = 3.0;
    private static final double RADIUS = 0.9;
    private static final double SLACK = 1.5;
    public static final float COST = 25F;

    private InfiltrateMove() {
        super("infiltrate", 0, 0F);
    }

    @Override
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        if (PillarMen.hiding(player)) {
            return null;
        }
        String busy = super.unavailable(player, data);
        if (busy != null) {
            return busy;
        }
        if (Possession.possessing(player)) {
            return "message.jojoha.possession.busy";
        }
        if (data.specEnergy < COST) {
            return "message.jojoha.skill.no_energy";
        }
        LivingEntity host = PillarAim.first(player, REACH, RADIUS);
        if (host == null || !(host instanceof Mob || host instanceof Player)) {
            return "message.jojoha.infiltrate.nothing";
        }
        if (PillarAim.boss(host)) {
            return "message.jojoha.infiltrate.too_vast";
        }
        return Possession.canTake(player, host);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        if (PillarMen.hiding(player)) {
            PillarMen.burst(player);
            data.setMoveCooldown(ID, player.level().getGameTime(), PillarMen.INFILTRATE_COOLDOWN);
            return;
        }
        LivingEntity host = PillarAim.first(player, REACH, RADIUS);
        if (host == null) {
            return;
        }
        data.specEnergy -= COST;
        MovementFx.pose(player, MovementFx.PALM);
        NetworkHandler.sendFirstPersonPose(player, MovementFx.PALM);
        NetworkHandler.sendPillarFx(player.serverLevel(), PillarFxKind.REACH_IN, player.getId(), host.getId(),
                MovementFx.PALM_CONTACT_TICKS);
        StandBeat.after(MovementFx.PALM_CONTACT_TICKS, player, host, (diver, body) -> {
            if (diver.distanceTo(body) > REACH + SLACK || PillarMen.hiding(diver) || PillarMen.petrified(diver)
                    || Possession.possessing(diver) || Possession.canTake(diver, body) != null) {
                return;
            }
            PillarMen.enter(diver, body);
        });
    }
}
