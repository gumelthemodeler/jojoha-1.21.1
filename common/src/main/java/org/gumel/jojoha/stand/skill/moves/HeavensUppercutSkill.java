package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class HeavensUppercutSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "heavens_uppercut");
    public static final HeavensUppercutSkill INSTANCE = new HeavensUppercutSkill();

    public static final int WINDUP_TICKS = StandEntity.STRIKE_CONTACT_TICKS;
    public static final float DAMAGE = 14F;
    public static final double LAUNCH = 1.15;
    public static final float PILLAR_HEIGHT = 8F;

    private static final int COOLDOWN_TICKS = 120;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private HeavensUppercutSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.heavens_uppercut";
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (stand == null) {
            return false;
        }
        LivingEntity aimed = SkillTargeting.lookTarget(player);
        if (aimed != null) {
            stand.triggerStrikeAt(aimed, "uppercut");
        } else {
            stand.triggerStrikeForward("uppercut");
        }
        HeavenShow.on(stand, HeavenFxPacket.CHARGE, 0F, WINDUP_TICKS);
        player.serverLevel().playSound(null, stand.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.6F);
        StandBeat.sweep(WINDUP_TICKS, player, SkillTargeting.REACH, HeavensUppercutSkill::land,
                () -> Whiff.stand(player));
        return true;
    }

    private static void land(ServerPlayer player, LivingEntity target) {
        Vec3 motion = target.getDeltaMovement();
        if (HeavenStrikes.strike(player, target, "heavens_uppercut", DAMAGE, new Vec3(motion.x * 0.2, LAUNCH,
                motion.z * 0.2), ImpactSpec.HEAVY, true)) {
            Stun.stagger(target);
        }
        HeavenShow.full(player.serverLevel(), HeavenFxPacket.UPPER, player.getId(), target.getId(),
                target.position(), PILLAR_HEIGHT, 0);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS,
                0.7F, 1.4F);
        player.serverLevel().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.2F, 0.6F);
    }
}
