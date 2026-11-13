package org.gumel.jojoha.stand.passive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;

public final class OpportunistPassive implements StandPassive {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "opportunist");

    public static final OpportunistPassive INSTANCE = new OpportunistPassive();

    private static final float BONUS = 0.2F;

    private static final int RECENT = 80;

    private OpportunistPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.opportunist";
    }

    @Override
    public float onRangedHit(ServerPlayer player, JojohaPlayerData data, LivingEntity target, Vec3 from,
                             float amount) {
        if (!fightingAnother(target, player)) {
            return amount;
        }
        player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(),
                target.getY() + target.getBbHeight() * 0.6, target.getZ(), 6, 0.25, 0.3, 0.25, 0.15);
        return amount * (1F + BONUS);
    }

    public static boolean fightingAnother(LivingEntity target, ServerPlayer user) {
        if (target instanceof Mob mob && other(mob.getTarget(), user)) {
            return true;
        }
        if (target.tickCount - target.getLastHurtByMobTimestamp() < RECENT && other(target.getLastHurtByMob(), user)) {
            return true;
        }
        return target.tickCount - target.getLastHurtMobTimestamp() < RECENT && other(target.getLastHurtMob(), user);
    }

    private static boolean other(LivingEntity someone, ServerPlayer user) {
        return someone != null && someone != user && someone.isAlive()
                && !(someone instanceof StandEntity stand && stand.getOwner() == user);
    }
}
