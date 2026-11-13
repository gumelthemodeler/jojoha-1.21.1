package org.gumel.jojoha.hamon.moves;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.fx.MovementFx;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.hamon.HamonFx;
import org.gumel.jojoha.hamon.HamonMove;
import org.gumel.jojoha.network.NetworkHandler;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.skill.StandBeat;

import java.util.List;

public final class YouthfulBreathMove implements HamonMove {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "youthful_breath");
    public static final YouthfulBreathMove INSTANCE = new YouthfulBreathMove();

    public static final int REGENERATION_TICKS = 100;
    public static final int REGENERATION_LEVEL = 1;
    public static final int CONTACT_TICKS = 5;

    public static final List<Holder<MobEffect>> SHED = List.of(MobEffects.MOVEMENT_SLOWDOWN, MobEffects.HUNGER,
            MobEffects.WEAKNESS, MobEffects.DIG_SLOWDOWN);

    private static final int COOLDOWN_TICKS = 600;
    private static final float ENERGY_COST = 20F;

    private YouthfulBreathMove() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
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
    public boolean activate(ServerPlayer player, JojohaPlayerData data) {
        MovementFx.pose(player, MovementFx.OVERDRIVE);
        NetworkHandler.sendFirstPersonPose(player, "gather");
        HamonFx.sound(player.serverLevel(), player, 0.5F, 0.8F);
        StandBeat.at(CONTACT_TICKS, player, () -> breathe(player));
        return true;
    }

    private static void breathe(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        for (Holder<MobEffect> effect : SHED) {
            player.removeEffect(effect);
        }
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_TICKS, REGENERATION_LEVEL));

        double x = player.getX(), y = player.getY(), z = player.getZ();
        for (int i = 0; i < 3; i++) {
            HamonFx.fineMote(level, x, y + 0.4 + i * 0.6, z, 6, 0.4);
        }
        HamonFx.ring(level, x, y + 0.1, z, 1.6, 1);
        level.playSound(null, player.blockPosition(), ModSounds.HAMON_EXHALE.get(), SoundSource.PLAYERS,
                0.6F, 1.4F);
    }
}
