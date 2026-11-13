package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandAim;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandTypes;
import org.gumel.jojoha.stand.StandVoices;
import org.gumel.jojoha.stand.TrustTier;
import org.gumel.jojoha.stand.passive.StandPassives;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class StarFingerSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "star_finger");
    public static final StarFingerSkill INSTANCE = new StarFingerSkill();

    public static final TagKey<Block> POKES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "star_finger_pokes"));

    private static final int COOLDOWN_TICKS = 45;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();
    public static final double REACH = 10.0;
    private static final float DAMAGE = 6.0F;

    public static final int CONTACT_TICKS = 9;

    private static final double PIERCE_PUSH = 0.9;

    private static final double PIERCE_LIFT = 0.2;

    private static final double TRAIL_STEP = 0.5;

    private static final double AIM_HEIGHT = 0.6;

    private StarFingerSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.star_finger";
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
    public TrustTier minimumTrust() {
        return TrustTier.BONDED;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        LivingEntity aimed = SkillTargeting.lookTarget(player, REACH);
        if (aimed != null) {
            stand.triggerStarFingerPose(aimed);
        } else {
            stand.triggerStarFingerAt(sight(player).getLocation());
        }

        player.serverLevel().playSound(null, player.blockPosition(), ModSounds.SP_STAR_FINGER.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StandBeat.sweep(CONTACT_TICKS, player, REACH,
                (attacker, hit) -> land(attacker, data, hit,
                        StandPassives.scaleOutgoing(attacker, data, hit,
                                StandTuning.damage("star_finger", DAMAGE) * data.stand.powerScale())),
                () -> {
                    if (!poke(player)) {
                        Whiff.stand(player);
                        BlockHitResult wall = sight(player);
                        if (wall.getType() == HitResult.Type.BLOCK) {
                            trail(player, wall.getLocation());
                            Impacts.landed(player.serverLevel(), wall.getLocation(), player.getViewVector(1.0F),
                                    ImpactSpec.LIGHT, Impacts.colourOf(player));
                        }
                    }
                });
        return true;
    }

    private static BlockHitResult sight(ServerPlayer player) {
        Vec3 eye = StandAim.eye(player);
        return player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(1.0F).scale(REACH)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private static boolean poke(ServerPlayer player) {
        BlockHitResult hit = sight(player);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (!state.is(POKES) || !level.mayInteract(player, hit.getBlockPos()) || player.isSpectator()) {
            return false;
        }
        return state.useWithoutItem(level, player, hit).consumesAction();
    }

    private static void land(ServerPlayer player, JojohaPlayerData data, LivingEntity target, float damage) {
        ServerLevel level = player.serverLevel();

        if (player.distanceTo(target) > REACH) {
            return;
        }

        Vec3 heading = player.getViewVector(1.0F);
        Vec3 chest = target.position().add(0, target.getBbHeight() * AIM_HEIGHT, 0);
        if (target.hurt(level.damageSources().playerAttack(player), DamageScale.stand(player, target, damage))) {
            Stun.stagger(target);
            target.setDeltaMovement(target.getDeltaMovement().add(heading.x * PIERCE_PUSH, PIERCE_LIFT,
                    heading.z * PIERCE_PUSH));
            target.hurtMarked = true;
            SoundEvent crack = StandVoices.heavyHit(StandTypes.byIdOrDefault(data.stand.standId()),
                    StandVoices.shadowed(data.stand));
            if (crack != null) {
                level.playSound(null, target.blockPosition(), crack, SoundSource.PLAYERS, 1.0F, 1.2F);
            }
        }
        trail(player, chest);
        Impacts.landed(level, target, chest, heading, ImpactSpec.HEAVY, Impacts.colourOf(player));
    }

    private static void trail(ServerPlayer player, Vec3 tip) {
        StandEntity stand = StandEntity.findFor(player);
        Vec3 from = stand != null ? stand.position().add(0, stand.getBbHeight() * AIM_HEIGHT, 0)
                : StandAim.eye(player);
        Vec3 line = tip.subtract(from);
        int steps = (int) (line.length() / TRAIL_STEP);
        for (int i = 1; i <= steps; i++) {
            Vec3 at = from.add(line.scale(i / (double) steps));
            player.serverLevel().sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
