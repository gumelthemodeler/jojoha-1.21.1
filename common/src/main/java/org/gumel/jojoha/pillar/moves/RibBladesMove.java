package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.pillar.PillarAim;
import org.gumel.jojoha.pillar.PillarFx;
import org.gumel.jojoha.pillar.PillarFxKind;
import org.gumel.jojoha.pillar.PillarMove;
import org.gumel.jojoha.pillar.PillarPoses;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireFx;

public final class RibBladesMove extends PillarMove {
    public static final ResourceLocation ID = id("rib_blades");
    public static final RibBladesMove INSTANCE = new RibBladesMove();

    public static final float DAMAGE = 6.0F;
    public static final double RADIUS = 2.0;
    private static final int RIBS = 10;
    private static final int HOLD_TICKS = 6;

    private RibBladesMove() {
        super("rib_blades", 120, 18F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.RIB_BLADES);
        PillarFx.charge(player, PillarFx.FLESH_STYLE, PillarPoses.RIB_BLADES_CONTACT_TICKS);
        ServerLevel level = player.serverLevel();
        StandBeat.during(PillarPoses.RIB_BLADES_CONTACT_TICKS - 6, PillarPoses.RIB_BLADES_CONTACT_TICKS - 1, player, tick -> {
            Vec3 chest = player.position().add(0, 1.1, 0);
            PillarFx.spray(level, PillarFx.bone(), chest, 1, 0.25, 0.02);
            if (tick % 3 == 0) {
                level.playSound(null, player.blockPosition(), SoundEvents.BONE_BLOCK_STEP, SoundSource.PLAYERS, 0.8F,
                        0.6F + tick * 0.05F);
            }
        });
        StandBeat.at(PillarPoses.RIB_BLADES_CONTACT_TICKS, player, () -> burst(player));
    }

    private static void burst(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 chest = player.position().add(0, 1.1, 0);
        for (LivingEntity target : PillarAim.around(player, chest, RADIUS)) {
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Bleed.apply(target);
            Impacts.landed(level, target, target.position().subtract(player.position()), ImpactSpec.MEDIUM.silent(),
                    Impacts.BLOOD);
        }
        NetworkHandler.sendPillarFx(level, PillarFxKind.RIB_BLADES, player.getId(), 0, HOLD_TICKS);
        for (int rib = 0; rib < RIBS; rib++) {
            double angle = rib * Math.PI * 2 / RIBS + player.getYRot() * Math.PI / 180;
            Vec3 out = new Vec3(Math.cos(angle), 0.15, Math.sin(angle)).scale(0.3);
            PillarFx.fling(level, PillarFx.flesh(), chest, out);
        }
        PillarFx.spray(level, PillarFx.bone(), chest, 8, 0.3, 0.12);
        VampireFx.blood(level, chest.x, chest.y, chest.z, 20, 0.4, 0.15);
        level.playSound(null, player.blockPosition(), SoundEvents.SKELETON_HURT, SoundSource.PLAYERS, 1.0F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.7F);
    }
}
