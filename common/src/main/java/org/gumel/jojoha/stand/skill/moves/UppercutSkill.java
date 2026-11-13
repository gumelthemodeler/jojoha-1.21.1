package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class UppercutSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "uppercut");
    public static final UppercutSkill INSTANCE = new UppercutSkill();

    private static final int WINDUP_TICKS = StandEntity.STRIKE_CONTACT_TICKS;

    private static final int COOLDOWN_TICKS = 50;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private static final double UNARMED_REACH = 4.0;
    private static final float STAND_DAMAGE = 6.0F;
    private static final float BODY_DAMAGE = 3.0F;
    private static final double STAND_LAUNCH = 1.05;
    private static final double BODY_LAUNCH = 0.72;

    private static final double RISE_CLOSE = 0.2;
    private static final double RISE_CLOSE_MAX = 0.5;

    private static final double RISE_SHARE = 0.95;

    private static final int BODY_CONTACT_TICKS = 7;

    private UppercutSkill() {
    }

    private static void rise(ServerPlayer player, LivingEntity target, double launch, boolean withStand) {
        Vec3 toward = target.position().subtract(player.position());
        Vec3 flat = new Vec3(toward.x, 0, toward.z).scale(RISE_CLOSE);
        if (flat.length() > RISE_CLOSE_MAX) {
            flat = flat.normalize().scale(RISE_CLOSE_MAX);
        }
        Vec3 lift = new Vec3(flat.x, launch * RISE_SHARE, flat.z);
        player.setDeltaMovement(lift);
        player.connection.send(new ClientboundSetEntityMotionPacket(player.getId(), lift));
        player.hurtMarked = true;
        player.fallDistance = 0F;
        MovementFx.play(player, MovementFx.Kind.RISE,
                new Vec3(0, 1, 0), Impacts.PLAYER, 6);
        if (withStand) {
            MovementFx.pose(player, MovementFx.DOUBLE_JUMP);
        }
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.uppercut";
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
    public boolean requiresStand() {
        return false;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        boolean withStand = stand != null;
        double reach = withStand ? SkillTargeting.REACH : UNARMED_REACH;

        LivingEntity aimed = SkillTargeting.lookTarget(player, reach);

        boolean rising = player.isCrouching();
        if (withStand) {
            if (aimed != null) {
                stand.triggerStrikeAt(aimed, "uppercut");
            } else {
                stand.triggerStrikeForward("uppercut");
            }
            StandBeat.sweep(WINDUP_TICKS, player, reach,
                    (attacker, hit) -> land(attacker, hit, standDamage(attacker, data, hit), STAND_LAUNCH, rising),
                    () -> Whiff.stand(player));
        } else {
            MovementFx.pose(player, MovementFx.UPPERCUT);
            player.swing(InteractionHand.MAIN_HAND, true);
            float damage = StandTuning.damage("uppercut_body", BODY_DAMAGE)
                    * StatEffects.bodyDamageScale(data.strength);
            StandBeat.sweep(BODY_CONTACT_TICKS, player, reach,
                    (attacker, hit) -> land(attacker, hit, damage, BODY_LAUNCH, rising),
                    () -> Whiff.body(player));
        }
        return true;
    }

    private static float standDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity hit) {
        return StandPassives.scaleOutgoing(player, data, hit,
                StandTuning.damage("uppercut_stand", STAND_DAMAGE) * data.stand.powerScale());
    }

    private static void land(ServerPlayer player, LivingEntity target, float damage, double launch, boolean rising) {
        ServerLevel level = player.serverLevel();

        if (target.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, target, damage))) {
            Stun.stagger(target);
        }

        Vec3 motion = target.getDeltaMovement();
        target.setDeltaMovement(motion.x, launch, motion.z);
        target.hurtMarked = true;
        if (rising) {
            rise(player, target, launch, launch == STAND_LAUNCH);
        }

        boolean withStand = launch == STAND_LAUNCH;
        Impacts.landed(level, target, new Vec3(0, 1, 0),
                withStand
                        ? ImpactSpec.HEAVY
                        : ImpactSpec.MEDIUM,
                withStand
                        ? Impacts.colourOf(player)
                        : Impacts.PLAYER);
    }
}
