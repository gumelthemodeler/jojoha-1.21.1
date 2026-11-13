package org.gumel.jojoha.pillar.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.Burning;
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

import java.util.List;

public final class BoilingBloodNeedlesMove extends PillarMove {
    public static final ResourceLocation ID = id("boiling_blood_needles");
    public static final BoilingBloodNeedlesMove INSTANCE = new BoilingBloodNeedlesMove();

    public static final double RANGE = 6.0;
    public static final double DOT = 0.6;
    public static final int TARGETS = 3;
    public static final float DAMAGE = 5.0F;
    private static final int HOLD_TICKS = 12;
    private static final int BOIL_TICKS = 40;

    private BoilingBloodNeedlesMove() {
        super("boiling_blood_needles", 120, 20F);
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, PillarPoses.BOILING_BLOOD_NEEDLES);
        PillarFx.charge(player, PillarFx.HEAT_STYLE, PillarPoses.BOILING_BLOOD_NEEDLES_CONTACT_TICKS);
        StandBeat.at(PillarPoses.BOILING_BLOOD_NEEDLES_CONTACT_TICKS, player, () -> lance(player));
    }

    private static void lance(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 hand = PillarAim.hand(player);
        List<LivingEntity> targets = PillarAim.cone(player, RANGE, DOT, TARGETS);

        int finger = 0;
        for (LivingEntity target : targets) {
            Vec3 at = target.getBoundingBox().getCenter();
            NetworkHandler.sendPillarFx(level, PillarFxKind.NEEDLE, player.getId(), target.getId(),
                    HOLD_TICKS + finger);
            NetworkHandler.sendPillarFx(level, PillarFxKind.BOIL, player.getId(), target.getId(), BOIL_TICKS);
            finger++;
            target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
            Burning.apply(target);
            Impacts.landed(level, target, at.subtract(hand), ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
            PillarFx.spray(level, PillarFx.bubble(), at, 6, 0.25, 0.04);
            PillarFx.spray(level, PillarFx.steam(), at, 3, 0.2, 0.03);
        }
        if (targets.isEmpty()) {
            PillarFx.spray(level, PillarFx.steam(), hand.add(player.getLookAngle().scale(1.5)), 4, 0.15, 0.03);
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.8F, 1.3F);
        level.playSound(null, player.blockPosition(), SoundEvents.LAVA_POP, SoundSource.PLAYERS, 1.0F, 0.8F);
    }
}
