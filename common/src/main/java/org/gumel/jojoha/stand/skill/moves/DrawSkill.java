package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Bleed;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Defence;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.anubis.Adaptation;
import org.gumel.jojoha.stand.anubis.Anubis;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class DrawSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "draw");
    public static final DrawSkill INSTANCE = new DrawSkill();

    private static final int COOLDOWN_TICKS = 30;

    private static final double LUNGE = 1.1;

    private static final double REACH = 3.5;
    private static final double AIM_DOT = 0.55;

    private static final float DAMAGE_SHARE = 0.8F;
    private static final float DAMAGE_LEAST = 4.0F;
    private static final float DAMAGE_MOST = 7.0F;

    private static final int WINDOW_TICKS = 8;

    private static final float COUNTER_DAMAGE = 6.0F;
    private static final float COUNTER_KNOWN = 2.0F;

    private static final int WEAR = 1;

    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final Map<UUID, Long> DRAWING = new HashMap<>();

    private DrawSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.draw";
    }

    @Override
    public boolean isUnlocked(JojohaPlayerData data) {
        return Anubis.is(data);
    }

    @Override
    public boolean requiresStand() {
        return false;
    }

    @Override
    public boolean passiveOnly() {
        return true;
    }

    @Override
    public boolean isAnswerToBeingHit() {
        return true;
    }

    @Override
    public boolean movesTheUsersBody() {
        return true;
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
    public String unavailable(ServerPlayer player, JojohaPlayerData data) {
        return Anubis.out(player) ? null : "message.jojoha.anubis.no_blade";
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();

        int window = WINDOW_TICKS + Anubis.windowBonus(player);
        if (Defence.openParry(player, window)) {
            DRAWING.put(player.getUUID(), now + window);
        }

        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        Vec3 along = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        player.setDeltaMovement(player.getDeltaMovement().add(along.x * LUNGE, 0.05, along.z * LUNGE));
        player.hurtMarked = true;

        player.swing(InteractionHand.MAIN_HAND, true);
        MovementFx.pose(player, MovementFx.RAPIER_LUNGE);
        level.playSound(null, player.blockPosition(), ModSounds.ANUBIS_REKKA.get(), SoundSource.PLAYERS, 0.9F, 1.0F);

        StandBeat.at(MovementFx.RAPIER_LUNGE_CONTACT_TICKS, player, () -> cut(player, look));

        player.getMainHandItem().hurtAndBreak(WEAR, player, EquipmentSlot.MAINHAND);
        return true;
    }

    private static void cut(ServerPlayer player, Vec3 look) {
        ServerLevel level = player.serverLevel();
        boolean throughWalls = Anubis.cutsThroughWalls(player);
        LivingEntity target = inFront(player, look, throughWalls);
        if (target != null) {
            float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SHARE;
            target.hurt(level.damageSources().playerAttack(player),
                    DamageScale.stand(player, target, Math.max(DAMAGE_LEAST, Math.min(DAMAGE_MOST, damage))));
            Impacts.landed(level, target, look,
                    ImpactSpec.MEDIUM.silent(), Impacts.STEEL);
            if (!player.hasLineOfSight(target)) {
                Anubis.slice(level, target);
            }
        }
    }

    private static LivingEntity inFront(ServerPlayer player, Vec3 look, boolean throughWalls) {
        Vec3 eye = player.getEyePosition();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(REACH),
                other -> other != player && other.isAlive() && !other.isSpectator()
                        && StandHits.hittable(player, other))) {
            Vec3 toward = candidate.getBoundingBox().getCenter().subtract(eye);
            double distance = toward.length();
            if (distance > REACH + candidate.getBbWidth() * 0.5 || distance < 1.0E-3) {
                continue;
            }
            if (toward.normalize().dot(look) < AIM_DOT || !(throughWalls || player.hasLineOfSight(candidate))) {
                continue;
            }
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    public static void parried(ServerPlayer defender, LivingEntity attacker) {
        Long until = DRAWING.remove(defender.getUUID());
        long now = defender.level().getGameTime();
        if (until == null || now > until || until - now > WINDOW_TICKS * 4L || !attacker.isAlive()) {
            return;
        }
        float cut = COUNTER_DAMAGE + (Adaptation.foeLevel(defender, attacker) >= 3 ? COUNTER_KNOWN : 0F);
        if (attacker.hurt(defender.damageSources().playerAttack(defender), DamageScale.stand(defender, attacker, Math.min(cut, Anubis.CEILING)))) {
            Bleed.apply(attacker);
        }
        defender.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel level = defender.serverLevel();
        Vec3 toward = attacker.position().subtract(defender.position());
        Impacts.landed(level, attacker, toward,
                ImpactSpec.HEAVY.silent(), Impacts.STEEL);
        level.playSound(null, attacker.blockPosition(), ModSounds.ANUBIS_POMMEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    public static void forget(Player player) {
        DRAWING.remove(player.getUUID());
    }
}
