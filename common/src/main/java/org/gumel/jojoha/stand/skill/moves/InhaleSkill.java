package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.StatEffects;
import org.gumel.jojoha.registry.ModRegistries;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class InhaleSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "inhale");
    public static final InhaleSkill INSTANCE = new InhaleSkill();

    private static final int START_LOCK_TICKS = 10;

    private static final int RECOVERY_TICKS = 120;

    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private static final int IFRAME_TICKS = 25;

    private static final int WIND_LINKS = 4;
    private static final double WIND_ROW_LENGTH = 7.0;
    private static final double WIND_JITTER = 0.55;
    private static final double WIND_SPEED = 0.55;

    private static final int SMOKE_PUFFS = 6;
    private static final double SMOKE_SPREAD = 2.2;

    private InhaleSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.inhale";
    }

    @Override
    public int cooldownTicks() {
        return START_LOCK_TICKS;
    }

    static int recoveryTicks(JojohaPlayerData data) {
        return Math.max(1, Math.round(RECOVERY_TICKS
                * StatEffects.standCooldownScale(data.stand.speed())));
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
        if (player instanceof ServerPlayer serverPlayer) {
            return InhaleChannel.isActive(serverPlayer);
        }
        StandEntity stand = StandEntity.findFor(player);
        return stand != null && stand.isInhaling();
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        if (InhaleChannel.isActive(player)) {
            InhaleChannel.end(player);
            return true;
        }
        if (stand == null) {
            return false;
        }

        ServerLevel level = player.serverLevel();
        boolean blowing = player.isShiftKeyDown();

        InhaleChannel.begin(player, stand, blowing);
        data.inhaleIFrameTicks = IFRAME_TICKS;

        Vec3 axis = player.getLookAngle();
        Vec3 origin = stand.position().add(0, stand.getBbHeight() * 0.5, 0);
        spawnWindRow(level, origin, axis, blowing);
        spawnSmoke(level, origin, axis, blowing);
        if (blowing) {
            level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                    SoundSource.PLAYERS, 0.8F, 1.3F);
        } else {
            level.playSound(null, player.blockPosition(), ModSounds.SP_INHALE.get(),
                    SoundSource.PLAYERS, 0.9F, 1.0F);
        }
        return true;
    }

    static void spawnWindRow(ServerLevel level, Vec3 origin, Vec3 look, boolean blowing) {
        RandomSource random = level.getRandom();

        Vec3 side = look.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();

        Vec3 travel = look.scale(blowing ? WIND_SPEED : -WIND_SPEED);

        for (int i = 0; i < WIND_LINKS; i++) {
            double along = (i + 1) / (double) WIND_LINKS * WIND_ROW_LENGTH;
            Vec3 at = origin.add(look.scale(along))
                    .add(side.scale((random.nextDouble() - 0.5) * WIND_JITTER))
                    .add(up.scale((random.nextDouble() - 0.5) * WIND_JITTER));

            level.sendParticles(ModRegistries.INHALE_WIND.get(), at.x, at.y, at.z, 0,
                    travel.x, travel.y, travel.z, 1.0);
        }
    }

    static void spawnSmoke(ServerLevel level, Vec3 origin, Vec3 look, boolean blowing) {
        Vec3 centre = origin.add(look.scale(blowing ? 1.2 : WIND_ROW_LENGTH * 0.6));
        RandomSource random = level.getRandom();

        for (int i = 0; i < SMOKE_PUFFS; i++) {
            double x = centre.x + (random.nextDouble() - 0.5) * SMOKE_SPREAD;
            double y = centre.y + (random.nextDouble() - 0.5) * SMOKE_SPREAD;
            double z = centre.z + (random.nextDouble() - 0.5) * SMOKE_SPREAD;

            level.sendParticles(ModRegistries.INHALE_SMOKE.get(), x, y, z, 0,
                    0.0, blowing ? -1.0 : 1.0, 0.0, 1.0);
        }
    }
}
