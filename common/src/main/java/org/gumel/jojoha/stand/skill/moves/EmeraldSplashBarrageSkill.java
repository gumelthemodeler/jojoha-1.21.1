package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.accessory.Relics;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.EmeraldSplash;
import org.gumel.jojoha.stand.hierophant.HierophantAim;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class EmeraldSplashBarrageSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "emerald_splash_barrage");
    public static final EmeraldSplashBarrageSkill INSTANCE = new EmeraldSplashBarrageSkill();

    private static final int CHARGE_TICKS = 30;

    private static final int LEAST_SHARDS = 6;
    private static final int MOST_SHARDS = 20;
    private static final float DAMAGE = 2.5F;
    private static final float SPEED = 2.4F;
    private static final float SCATTER = 3.0F;
    private static final double REACH = 32.0;

    public static final float FINAL_HEALTH = 0.3F;
    private static final int FINAL_SHARDS = 32;
    private static final float FINAL_DAMAGE = 4F;
    private static final int FINAL_COOLDOWN_TICKS = 900;

    private static final int STREAM_START_TICKS = 24;
    private static final int STREAM_LOOP_TICKS = 13;

    private static final int COOLDOWN_TICKS = 160;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private EmeraldSplashBarrageSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.emerald_splash_barrage";
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
    public int chargeMaxTicks() {
        return CHARGE_TICKS;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand) {
        return activate(player, data, stand, 0);
    }

    public static boolean finalSplash(ServerPlayer player) {
        return player.getHealth() <= player.getMaxHealth() * FINAL_HEALTH;
    }

    @Override
    public boolean activate(ServerPlayer player, JojohaPlayerData data, StandEntity stand, int chargeTicks) {
        if (stand == null) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        boolean last = finalSplash(player);
        float scale = data.stand.powerScale();
        float damage = Math.min(10F, StandTuning.damage(last ? "final_emerald_splash" : "emerald_splash_barrage",
                last ? FINAL_DAMAGE : DAMAGE) * scale);
        float charged = Math.min(1F, chargeTicks / (float) CHARGE_TICKS);
        int shards = (last ? FINAL_SHARDS : Math.round(LEAST_SHARDS + (MOST_SHARDS - LEAST_SHARDS) * charged))
                + Relics.extraEmeralds(player);

        stand.triggerStrikeForward("barrage");
        if (last) {
            player.displayClientMessage(Component.translatable("message.jojoha.final_emerald_splash"), true);
            level.playSound(null, stand.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    1.2F, 0.6F);
            StandBeat.at(1, player, () -> {
                JojohaPlayerData after = PlayerDataAccess.get(player);
                after.setMoveCooldown(ID, player.level().getGameTime(), FINAL_COOLDOWN_TICKS);
                PlayerDataAccess.set(player, after);
                PlayerDataAccess.sync(player);
            });
        }

        StandBeat.during(1, shards, player, tick -> {
            if (!stand.isAlive()) {
                return;
            }
            Vec3 from = HierophantAim.hands(player, stand);
            Vec3 heading = HierophantAim.crosshair(player, REACH).subtract(from).normalize();
            EmeraldSplash.fire(level, player, from, heading, SPEED, last ? SCATTER * 1.5F : SCATTER, damage, last);
            if (last) {
                EmeraldSplash.fire(level, player, from, heading, SPEED, SCATTER * 2F, damage, true);
            }
            if (tick == 1) {
                level.playSound(null, stand.blockPosition(), ModSounds.HG_EMERALD_START.get(),
                        SoundSource.PLAYERS, 1.0F, 1.0F);
                if (last) {
                    level.playSound(null, player.blockPosition(), ModSounds.HG_YELL.get(),
                            SoundSource.PLAYERS, 1.2F, 1.0F);
                }
            } else if (tick >= STREAM_START_TICKS && (tick - STREAM_START_TICKS) % STREAM_LOOP_TICKS == 0) {
                level.playSound(null, stand.blockPosition(), ModSounds.HG_EMERALD_CONTINUE.get(),
                        SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            if (tick % 3 == 1) {
                EmeraldSplash.muzzle(level, player, from, heading, last ? 2 : 1);
                level.playSound(null, stand.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS,
                        0.8F, 1.2F + level.getRandom().nextFloat() * 0.4F);
            }
        });
        return true;
    }
}
