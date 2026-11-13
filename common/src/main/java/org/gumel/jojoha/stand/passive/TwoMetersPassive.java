package org.gumel.jojoha.stand.passive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.StandSummonHandler;

public final class TwoMetersPassive implements StandPassive {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "two_meters");

    private static final double POINT_BLANK = 2.0;
    private static final double FALLOFF = 9.0;

    private static final float MAX_MULTIPLIER = 1.3F;

    private static final int COOLDOWN_TICKS = 80;

    public static final TwoMetersPassive INSTANCE = new TwoMetersPassive();

    private TwoMetersPassive() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "passive." + ID.getNamespace() + "." + ID.getPath();
    }

    @Override
    public float onOutgoingDamage(ServerPlayer player, JojohaPlayerData data, LivingEntity target,
                                  float amount) {
        StandEntity stand = StandSummonHandler.findStand(player, data);
        if (stand == null) {
            return amount;
        }

        long now = player.level().getGameTime();
        if (data.isMoveOnCooldown(ID, now)) {
            return amount;
        }
        float multiplier = multiplierAt(player.distanceTo(target), POINT_BLANK);
        if (multiplier <= 1F) {
            return amount;
        }
        data.setMoveCooldown(ID, now,
                StandTuning.ticks("two_meters_cooldown_ticks", COOLDOWN_TICKS));
        return amount * multiplier;
    }

    public static float multiplierAt(double distance) {
        return multiplierAt(distance, POINT_BLANK);
    }

    public static float multiplierAt(double distance, double pointBlank) {
        float max = StandTuning.value("two_meters_max_multiplier", MAX_MULTIPLIER);
        if (distance <= pointBlank) {
            return max;
        }
        if (distance >= FALLOFF) {
            return 1F;
        }

        float t = (float) ((distance - pointBlank) / (FALLOFF - pointBlank));
        float eased = t * t * (3F - 2F * t);
        return Mth.lerp(eased, max, 1F);
    }
}
