package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.entity.StandKnife;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class KnifeHaloSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "knife_halo");
    public static final KnifeHaloSkill INSTANCE = new KnifeHaloSkill();

    public static final int KNIVES = 12;
    public static final double RADIUS = 1.5;
    public static final double LIFT = 0.9;
    public static final double BACK = 0.8;
    public static final int MOST_HOLD_TICKS = 100;
    public static final double SPIN = 0.16;
    public static final float KNIFE_DAMAGE = 5F;
    public static final double SPEED = 2.4;
    public static final double REACH = 24.0;

    private static final Map<UUID, Halo> HALOS = new HashMap<>();

    private static final class Halo {
        private final List<StandKnife> knives;
        private double turn;

        private Halo(List<StandKnife> knives) {
            this.knives = knives;
        }
    }

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private KnifeHaloSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.knife_halo";
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
    public boolean isSustained() {
        return true;
    }

    @Override
    public boolean isSustainActive(Player player) {
        return HALOS.containsKey(player.getUUID());
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        Halo held = HALOS.remove(player.getUUID());
        if (held != null) {
            loose(player, held.knives);
            return true;
        }
        if (stand == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        float perKnife = DamageScale.stand(player, null,
                StandTuning.damage("knife_halo", KNIFE_DAMAGE) * data.stand.powerScale());
        float base = (float) (perKnife / SPEED);
        Vec3 centre = centre(player);

        List<StandKnife> halo = new ArrayList<>();
        for (int i = 0; i < KNIVES; i++) {
            Vec3 spot = spot(player, centre, i, 0.0);
            StandKnife knife = new StandKnife(level, player, base);
            knife.sparesOwner();
            knife.wounding();
            knife.gild();
            knife.setNoGravity(true);
            knife.setPos(spot.x, spot.y, spot.z);
            knife.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(knife);
            halo.add(knife);
        }
        Halo ring = new Halo(halo);
        HALOS.put(player.getUUID(), ring);
        stand.triggerKnifeThrowPose();
        HeavenShow.at(level, HeavenFxPacket.KNIVES, centre, (float) RADIUS, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                1.2F, 1.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS,
                1.0F, 0.6F);

        StandBeat.during(1, MOST_HOLD_TICKS - 1, player, tick -> {
            if (HALOS.get(player.getUUID()) != ring) {
                return;
            }
            if (!player.isAlive() || player.isRemoved()) {
                HALOS.remove(player.getUUID());
                halo.forEach(StandKnife::discard);
                return;
            }
            ring.turn += SPIN;
            Vec3 now = centre(player);
            for (int i = 0; i < halo.size(); i++) {
                StandKnife knife = halo.get(i);
                if (knife.isAlive()) {
                    Vec3 next = spot(player, now, i, ring.turn);
                    knife.setDeltaMovement(next.subtract(knife.position()));
                    knife.hasImpulse = true;
                }
            }
        });
        StandBeat.at(MOST_HOLD_TICKS, player, () -> {
            if (HALOS.get(player.getUUID()) == ring) {
                HALOS.remove(player.getUUID());
                loose(player, halo);
            }
        });
        return true;
    }

    private static Vec3 centre(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        return player.getEyePosition().add(up(player).scale(LIFT)).subtract(look.scale(BACK));
    }

    private static Vec3 right(ServerPlayer player) {
        Vec3 right = player.getLookAngle().cross(new Vec3(0, 1, 0));
        return right.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : right.normalize();
    }

    private static Vec3 up(ServerPlayer player) {
        return right(player).cross(player.getLookAngle()).normalize();
    }

    private static Vec3 spot(ServerPlayer player, Vec3 centre, int index, double turn) {
        double angle = index * Math.PI * 2 / KNIVES + turn;
        return centre.add(right(player).scale(Math.cos(angle) * RADIUS)).add(up(player).scale(Math.sin(angle) * RADIUS));
    }

    private static Vec3 aim(ServerPlayer player) {
        LivingEntity target = SkillTargeting.lookTarget(player, REACH);
        return target != null ? target.position().add(0.0, target.getBbHeight() * 0.55, 0.0)
                : player.getEyePosition().add(player.getLookAngle().scale(REACH));
    }

    private static void loose(ServerPlayer player, List<StandKnife> halo) {
        ServerLevel level = player.serverLevel();
        Vec3 centre = centre(player);
        Vec3 aim = aim(player);
        for (StandKnife knife : halo) {
            if (!knife.isAlive()) {
                continue;
            }
            Vec3 heading = aim.subtract(knife.position());
            knife.setNoGravity(false);
            knife.shoot(heading.x, heading.y, heading.z, (float) SPEED, 0F);
        }
        HeavenShow.at(level, HeavenFxPacket.KNIVES, centre, (float) -RADIUS, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.2F, 1.1F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.5F);
    }
}
