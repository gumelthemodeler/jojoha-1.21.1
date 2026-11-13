package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModEffects;

public final class TangledPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "tangled");

    public static final TangledPassive INSTANCE = new TangledPassive();

    private static final float CHANCE = 0.18F;
    private static final int TANGLE_TICKS = 50;

    private static final int STUN_TICKS = 10;

    public static final int BLOCKED = 3;
    public static final int SLOTS = 8;

    private static final int[][] COMBINATIONS = combinations();

    private TangledPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive.jojoha.tangled";
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        if (!(player.level() instanceof ServerLevel level) || target.level().isClientSide()) {
            return amount;
        }

        if (target.hasEffect(ModEffects.tangled()) || level.random.nextFloat() >= CHANCE) {
            return amount;
        }

        int which = level.random.nextInt(COMBINATIONS.length);
        target.addEffect(new MobEffectInstance(ModEffects.tangled(), TANGLE_TICKS, which,
                false, false, true));

        target.addEffect(new MobEffectInstance(ModEffects.stun(), STUN_TICKS, Stun.TIER_TWO,
                false, false, true));

        return amount;
    }

    public static boolean blocks(int amplifier, int slot) {
        if (amplifier < 0 || amplifier >= COMBINATIONS.length) {
            return false;
        }

        for (int blocked : COMBINATIONS[amplifier]) {
            if (blocked == slot) {
                return true;
            }
        }
        return false;
    }

    private static int[][] combinations() {
        int count = 0;
        for (int a = 0; a < SLOTS - 2; a++) {
            for (int b = a + 1; b < SLOTS - 1; b++) {
                count += SLOTS - 1 - b;
            }
        }

        int[][] all = new int[count][BLOCKED];
        int at = 0;
        for (int a = 0; a < SLOTS - 2; a++) {
            for (int b = a + 1; b < SLOTS - 1; b++) {
                for (int c = b + 1; c < SLOTS; c++) {
                    all[at][0] = a;
                    all[at][1] = b;
                    all[at][2] = c;
                    at++;
                }
            }
        }
        return all;
    }
}
