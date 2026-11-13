package org.gumel.jojoha.vampire.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.network.packet.VampireMarkPacket;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.vampire.VampireAim;
import org.gumel.jojoha.vampire.VampireFx;
import org.gumel.jojoha.vampire.VampireMove;
import org.gumel.jojoha.vampire.VampirePoses;

public final class DrainMove implements VampireMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "drain");
    public static final DrainMove INSTANCE = new DrainMove();

    private static final double REACH = 4.5;

    private static final double SLACK = 1.5;
    private static final double RADIUS = 0.9;

    private static final float DAMAGE = 6.0F;
    private static final float HEAL = 4.0F;
    private static final int BLOOD_GAINED = 5;

    private static final int FEED_POINTS = 8;

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = 10F;

    private DrainMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public float energyCost() {
        return ENERGY_COST;
    }

    @Override
    public void activate(ServerPlayer player, JojohaPlayerData data) {
        ServerLevel level = player.serverLevel();
        LivingEntity target = VampireAim.firstAlong(player, REACH, RADIUS);

        if (target == null) {
            player.displayClientMessage(Component.translatable("message.jojoha.drain.nothing"), true);
            return;
        }

        if (!VampireBlood.hasBlood(target)) {
            player.displayClientMessage(Component.translatable("message.jojoha.drain.dry"), true);
            return;
        }

        MovementFx.pose(player, VampirePoses.DRAIN);
        StandBeat.after(VampirePoses.DRAIN_CONTACT_TICKS, player, target, (drinker, body) -> {
            if (drinker.distanceTo(body) > REACH + SLACK) {
                return;
            }
            drink(drinker.serverLevel(), drinker, body);
            thread(drinker.serverLevel(), drinker, body);
        });
    }

    private static final int PALE_TICKS = 40;

    private static void drink(ServerLevel level, ServerPlayer player, LivingEntity target) {
        target.hurt(level.damageSources().playerAttack(player), DamageScale.vampire(player, target, DAMAGE));
        Impacts.landed(level, target, target.position().subtract(player.position()), ImpactSpec.MEDIUM.silent(), Impacts.BLOOD);
        NetworkHandler.sendVampireMark(level, target,
                VampireMarkPacket.Mark.DRAINED, PALE_TICKS);
        player.heal(HEAL);
        VampireBlood.feed(player, BLOOD_GAINED);

        VampireFx.blood(level, target.getX(), target.getEyeY(), target.getZ(), 26, 0.16, 0.26);
        VampireFx.ring(level, target.getX(), target.getEyeY(), target.getZ(), 1.5, 1);
        VampireFx.ring(level, target.getX(), target.getEyeY(), target.getZ(), 0.8, 0);

        level.playSound(null, player.blockPosition(), ModSounds.VAMPIRE_SUCK.get(),
                SoundSource.PLAYERS, 0.9F, 0.7F);
    }

    private static void thread(ServerLevel level, ServerPlayer player, LivingEntity target) {
        Vec3 from = target.getEyePosition();
        Vec3 toward = player.getEyePosition().add(0.0, -0.2, 0.0).subtract(from);
        for (int i = 1; i <= FEED_POINTS; i++) {
            Vec3 at = from.add(toward.scale(i / (double) FEED_POINTS));
            VampireFx.blood(level, at.x, at.y, at.z, 3, 0.06, 0.02);
        }
    }
}
