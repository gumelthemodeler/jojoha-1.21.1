package org.gumel.jojoha.item;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.gumel.jojoha.combat.MovementCharges;
import org.gumel.jojoha.combat.VampireBlood;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StatGrowth;
import org.gumel.jojoha.data.StatPoints;
import org.gumel.jojoha.registry.ModEffects;

import java.util.ArrayList;
import java.util.List;

public final class Curios {
    public static final int TONIC_SECONDS = 180;
    public static final int TONIC_PARRY_TICKS = 1;

    public static final int GUM_SECONDS = 60;
    public static final float GUM_STAND_REGEN = 1.5F;

    public static final int WINE_SECONDS = 120;
    public static final float WINE_BREATH_REGEN = 1.5F;

    public static final int VIAL_BLOOD = 6;
    public static final int VIAL_NAUSEA_SECONDS = 8;

    public static final int COIN_LUCK_SECONDS = 300;
    public static final int COIN_UNLUCK_SECONDS = 60;

    public static final float PAGE_LEVEL_SHARE = 0.1F;

    public static final int ROAD_ROLLER_KEY_COINS = 8;

    public enum Arcana {
        MAGICIAN("magician", MobEffects.FIRE_RESISTANCE, 120, true),
        STRENGTH("strength", MobEffects.DAMAGE_BOOST, 60, true),
        CHARIOT("chariot", MobEffects.MOVEMENT_SPEED, 90, true),
        HERMIT("hermit", MobEffects.NIGHT_VISION, 180, true),
        STAR("star", MobEffects.REGENERATION, 20, true),
        SUN("sun", MobEffects.ABSORPTION, 120, true),
        MOON("moon", MobEffects.WATER_BREATHING, 120, true),
        FOOL("fool", MobEffects.SLOW_FALLING, 60, true),
        WORLD("world", MobEffects.DIG_SPEED, 60, true),
        TOWER("tower", MobEffects.WEAKNESS, 30, false),
        DEATH("death", MobEffects.WITHER, 5, false);

        private final String key;
        private final Holder<MobEffect> effect;
        private final int seconds;
        private final boolean kind;

        Arcana(String key, Holder<MobEffect> effect, int seconds, boolean kind) {
            this.key = key;
            this.effect = effect;
            this.seconds = seconds;
            this.kind = kind;
        }

        public String key() {
            return key;
        }
    }

    private Curios() {
    }

    private static void sound(ServerPlayer player, SoundEvent sound, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 0.8F, pitch);
    }

    private static boolean give(ServerPlayer player, Holder<MobEffect> effect, int seconds) {
        return player.addEffect(new MobEffectInstance(effect, seconds * 20, 0, false, false, true));
    }

    public static int parryBonus(ServerPlayer player) {
        return player.hasEffect(ModEffects.keenGuard()) ? TONIC_PARRY_TICKS : 0;
    }

    public static float standRegenScale(ServerPlayer player) {
        return player.hasEffect(ModEffects.standVigor()) ? GUM_STAND_REGEN : 1F;
    }

    public static float breathScale(ServerPlayer player) {
        return player.hasEffect(ModEffects.steadyBreath()) ? WINE_BREATH_REGEN : 1F;
    }

    public static boolean tonic(ServerPlayer player) {
        give(player, ModEffects.keenGuard(), TONIC_SECONDS);
        return true;
    }

    public static boolean gum(ServerPlayer player) {
        give(player, ModEffects.standVigor(), GUM_SECONDS);
        return true;
    }

    public static boolean wine(ServerPlayer player) {
        give(player, ModEffects.steadyBreath(), WINE_SECONDS);
        return true;
    }

    public static void spill(ServerPlayer player) {
        if (player.removeEffect(ModEffects.steadyBreath())) {
            sound(player, SoundEvents.GLASS_BREAK, 1.3F);
            player.displayClientMessage(Component.translatable("message.jojoha.zeppeli_wine.spilled"), true);
        }
    }

    public static boolean vial(ServerPlayer player) {
        if (VampireBlood.isVampire(player)) {
            VampireBlood.feed(player, VIAL_BLOOD);
        } else {
            give(player, MobEffects.CONFUSION, VIAL_NAUSEA_SECONDS);
            player.displayClientMessage(Component.translatable("message.jojoha.blood_vial.foul"), true);
        }
        return true;
    }

    public static boolean cherries(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        MovementCharges.refresh(data, MovementCharges.Kind.DASH);
        PlayerDataAccess.set(player, data);
        PlayerDataAccess.sync(player);
        return true;
    }

    public static boolean coin(ServerPlayer player) {
        boolean heads = player.getRandom().nextBoolean();
        sound(player, SoundEvents.CHAIN_PLACE, heads ? 1.8F : 1.2F);
        give(player, heads ? MobEffects.LUCK : MobEffects.UNLUCK, heads ? COIN_LUCK_SECONDS : COIN_UNLUCK_SECONDS);
        player.displayClientMessage(Component.translatable(
                heads ? "message.jojoha.lucky_coin.heads" : "message.jojoha.lucky_coin.tails"), true);
        return true;
    }

    public static boolean page(ServerPlayer player) {
        JojohaPlayerData data = PlayerDataAccess.get(player);
        List<int[]> open = new ArrayList<>();
        for (int stat = 0; stat < StatPoints.COUNT; stat++) {
            if (StatPoints.playerStat(data, stat) < StatPoints.MAX_STAT) {
                open.add(new int[]{0, stat});
            }
            if (data.stand.isPresent() && stat != StatPoints.STAND_ENDURANCE
                    && data.stand.stats()[stat] < StatPoints.MAX_STAT) {
                open.add(new int[]{1, stat});
            }
        }
        if (open.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.jojoha.stat_crystal.full"), true);
            return false;
        }
        int[] pick = open.get(player.getRandom().nextInt(open.size()));
        StatGrowth.feed(player, pick[0] == 1, pick[1], PAGE_LEVEL_SHARE, false);
        sound(player, SoundEvents.BOOK_PAGE_TURN, 1.0F);
        return true;
    }

    public static boolean tarot(ServerPlayer player) {
        Arcana drawn = Arcana.values()[player.getRandom().nextInt(Arcana.values().length)];
        give(player, drawn.effect, drawn.seconds);
        sound(player, SoundEvents.BOOK_PAGE_TURN, drawn.kind ? 1.4F : 0.7F);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(drawn.kind ? ParticleTypes.ENCHANT : ParticleTypes.SMOKE, player.getX(),
                    player.getY() + 1.2, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
        player.displayClientMessage(Component.translatable("message.jojoha.tarot_card.drawn",
                Component.translatable("arcana.jojoha." + drawn.key)), true);
        return true;
    }
}
