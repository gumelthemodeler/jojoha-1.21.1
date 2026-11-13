package org.gumel.jojoha.stand.passive;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.GuardBreak;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.stand.StandCombatHandler;
import org.gumel.jojoha.stand.chariot.ChariotBlade;

public final class PiercingThrustPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "piercing_thrust");
    public static final PiercingThrustPassive INSTANCE = new PiercingThrustPassive();

    public static final float CHANCE = 0.25F;
    public static final float PIERCE = 0.25F;
    public static final float SURE_PIERCE = 0.4F;

    private static boolean charged;

    private PiercingThrustPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.piercing_thrust";
    }

    public static void charged(Runnable blow) {
        boolean before = charged;
        charged = true;
        try {
            blow.run();
        } finally {
            charged = before;
        }
    }

    public static boolean exposed(LivingEntity target) {
        return Stun.isStunned(target) || GuardBreak.isBroken(target);
    }

    public static float armour(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player) || victim.getArmorValue() <= 0
                || !(source.getDirectEntity() instanceof ChariotBlade || StandCombatHandler.standBlow(source))
                || !StandPassives.grants(PlayerDataAccess.get(player), ID)) {
            return 0F;
        }
        boolean sure = charged || exposed(victim);
        if (!sure && player.getRandom().nextFloat() >= CHANCE) {
            return 0F;
        }
        player.serverLevel().sendParticles(ParticleTypes.ENCHANTED_HIT, victim.getX(),
                victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), sure ? 8 : 5, 0.25, 0.25, 0.25, 0.1);
        return sure ? SURE_PIERCE : PIERCE;
    }
}
