package org.gumel.jojoha.stand.skill.moves;

import dev.architectury.networking.NetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.combat.DamageScale;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.combat.fx.ImpactSpec;
import org.gumel.jojoha.combat.fx.Impacts;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.data.PlayerDataAccess;
import org.gumel.jojoha.data.StandTuning;
import org.gumel.jojoha.network.packet.HeavenFxPacket;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.heaven.Heaven;
import org.gumel.jojoha.stand.heaven.HeavenShow;
import org.gumel.jojoha.stand.heaven.Overwrite;
import org.gumel.jojoha.stand.heaven.Servitude;
import org.gumel.jojoha.stand.heaven.WorldPatch;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

import java.util.ArrayList;
import java.util.List;

public final class ErasureSkill implements StandSkill {
    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "erasure");
    public static final ErasureSkill INSTANCE = new ErasureSkill();

    public static final double RADIUS = 26.0;
    public static final int WARNING_TICKS = 70;
    public static final int VOID_SECONDS = 20;
    public static final int VOID_WAVE_TICKS = 16;
    public static final int HEAL_TICKS = 50;
    public static final double SHADER_SEEN = 160.0;
    public static final int FREEZE_TICKS = 30;
    public static final int LOCK_SECONDS = 8;
    public static final int ERASED_SECONDS = 4;
    public static final float REWRITE_DAMAGE = 12F;
    public static final float UNMAKE_FRACTION = 0.4F;
    public static final float BOSS_HEALTH = 100F;

    private static final int COOLDOWN_TICKS = 1200;
    private static final float ENERGY_COST = EnergyWeight.HEAVY.cost();

    private ErasureSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.erasure";
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
        ServerLevel level = player.serverLevel();
        Heaven.telegraph(player);
        Heaven.announce(player, "announce.jojoha.heaven_erasure");
        HeavenShow.on(player, HeavenFxPacket.DOME, (float) Heaven.range(RADIUS), WARNING_TICKS);
        HeavenShow.on(player, HeavenFxPacket.CRACKS, (float) Heaven.range(RADIUS), WARNING_TICKS);
        int radius = (int) Math.round(Heaven.range(RADIUS));
        int hold = VOID_SECONDS * 20;
        WorldPatch.begin(level, player.blockPosition(), radius, WARNING_TICKS - 10, WARNING_TICKS, VOID_WAVE_TICKS,
                hold, HEAL_TICKS, true, ErasureSkill::erasable, (at, pos, now) -> Blocks.AIR
                        .defaultBlockState());
        Vec3 centre = player.position();
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(centre) <= SHADER_SEEN * SHADER_SEEN) {
                NetworkManager.sendToPlayer(other, new HeavenFxPacket(HeavenFxPacket.ERASE_WORLD, other.getId(),
                        WARNING_TICKS, centre.x, centre.y, centre.z, radius, WARNING_TICKS + hold));
            }
        }
        StandBeat.during(1, WARNING_TICKS - 1, player, tick -> {
            if (tick % 10 == 0) {
                float building = tick / (float) WARNING_TICKS;
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                        1.5F + building, 0.5F + building * 0.4F);
                level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                        0.8F + building, 0.4F + building * 0.6F);
            }
        });
        StandBeat.at(WARNING_TICKS, player, () -> {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, hold, 0, false, false, true));
            erase(player);
        });
        if (stand != null) {
            stand.triggerTimeStopPose();
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.4F, 0.4F);
        level.playSound(null, player.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.4F, 1.6F);
        return true;
    }

    private static List<LivingEntity> victims(ServerPlayer player) {
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(Heaven.range(RADIUS)),
                entity -> entity != player && entity.isAlive() && !(entity instanceof StandEntity)
                        && !entity.isSpectator() && !Servitude.serves(entity)
                        && !(entity instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID())));
    }

    private static boolean erasable(ServerLevel level, BlockPos pos,
                                    BlockState state) {
        return !state.isAir() && state.getDestroySpeed(level, pos) >= 0F;
    }

    public static boolean boss(LivingEntity entity) {
        return entity instanceof EnderDragon || entity instanceof WitherBoss || entity instanceof Warden
                || entity instanceof ElderGuardian || entity.getMaxHealth() > BOSS_HEALTH;
    }

    public static boolean unmakeable(LivingEntity entity) {
        return !(entity instanceof Player) && !boss(entity)
                && entity.getHealth() <= entity.getMaxHealth() * UNMAKE_FRACTION;
    }

    private static void erase(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2F, 0.6F);
        int erased = ERASED_SECONDS * 20;
        List<LivingEntity> frozen = new ArrayList<>();
        for (LivingEntity victim : victims(player)) {
            if (unmakeable(victim)) {
                unmake(player, victim);
                continue;
            }
            strip(victim);
            Stun.lock(victim, FREEZE_TICKS);
            victim.setDeltaMovement(Vec3.ZERO);
            victim.hurtMarked = true;
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, erased, 1, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, erased, 1, false, true, true));
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, erased, 1, false, true, true));
            if (victim instanceof ServerPlayer erasedPlayer) {
                Overwrite.disable(erasedPlayer, LOCK_SECONDS * 20);
                erasedPlayer.displayClientMessage(Component.translatable("message.jojoha.heaven.stand_erased"), true);
            }
            HeavenShow.on(victim, HeavenFxPacket.ERASED, 0F, FREEZE_TICKS);
            frozen.add(victim);
        }
        StandBeat.at(FREEZE_TICKS, player, () -> rewrite(player, frozen));
    }

    private static void strip(LivingEntity victim) {
        List<Holder<MobEffect>> good = new ArrayList<>();
        for (MobEffectInstance effect : victim.getActiveEffects()) {
            if (effect.getEffect().value().isBeneficial()) {
                good.add(effect.getEffect());
            }
        }
        good.forEach(victim::removeEffect);
        victim.setAbsorptionAmount(0F);
    }

    private static void unmake(ServerPlayer player, LivingEntity victim) {
        ServerLevel level = player.serverLevel();
        HeavenShow.full(level, HeavenFxPacket.UNMADE, victim.getId(), -1, victim.position(), victim.getBbHeight(), 0);
        level.playSound(null, victim.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                1.2F, 0.5F);
        victim.invulnerableTime = 0;
        victim.hurt(player.damageSources().playerAttack(player), DamageScale.stand(player, victim, Float.MAX_VALUE));
    }

    private static void rewrite(ServerPlayer player, List<LivingEntity> frozen) {
        ServerLevel level = player.serverLevel();
        JojohaPlayerData data = PlayerDataAccess.get(player);
        level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.4F, 0.5F);
        float raw = StandTuning.damage("erasure", REWRITE_DAMAGE) * data.stand.powerScale();
        for (LivingEntity victim : frozen) {
            if (!victim.isAlive()) {
                continue;
            }
            victim.invulnerableTime = 0;
            victim.hurt(player.damageSources().playerAttack(player),
                    DamageScale.stand(player, victim, raw));
            Impacts.landed(level, victim, new Vec3(0, 1, 0),
                    ImpactSpec.HEAVY, Impacts.colourOf(player));
        }
    }
}
