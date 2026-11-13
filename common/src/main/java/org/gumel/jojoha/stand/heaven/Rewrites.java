package org.gumel.jojoha.stand.heaven;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.combat.StandHits;
import org.gumel.jojoha.combat.Stun;
import org.gumel.jojoha.registry.ModEffects;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.combat.BarrageDemolition;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Rewrites {
    public static final double PROJECTILE_RADIUS = 5.0;
    public static final int FREEZE_TICKS = 40;
    public static final int ITEM_LOCK_TICKS = 60;
    public static final int STRIKE_GAP_TICKS = 10;
    public static final float GROUND_CHANCE = 0.35F;

    private static final Map<UUID, Long> LAST = new HashMap<>();

    private static final Map<Block, Block> ALTERED = Map.ofEntries(
            Map.entry(Blocks.GRASS_BLOCK, Blocks.DIRT),
            Map.entry(Blocks.DIRT, Blocks.COARSE_DIRT),
            Map.entry(Blocks.STONE, Blocks.COBBLESTONE),
            Map.entry(Blocks.COBBLESTONE, Blocks.GRAVEL),
            Map.entry(Blocks.DEEPSLATE, Blocks.COBBLED_DEEPSLATE),
            Map.entry(Blocks.SAND, Blocks.SANDSTONE),
            Map.entry(Blocks.SANDSTONE, Blocks.SAND),
            Map.entry(Blocks.OAK_PLANKS, Blocks.STRIPPED_OAK_WOOD),
            Map.entry(Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS),
            Map.entry(Blocks.NETHERRACK, Blocks.BLACKSTONE));

    private Rewrites() {
    }

    public static void strike(ServerPlayer player, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        Long last = LAST.get(player.getUUID());
        if (last != null && now - last < STRIKE_GAP_TICKS) {
            return;
        }
        LAST.put(player.getUUID(), now);
        LivingEntity body = StandHits.bodyOf(target);

        eraseProjectiles(player, level, body.position());
        if (body instanceof Player victim) {
            lockItems(victim);
        } else if (body instanceof Mob) {
            body.addEffect(new MobEffectInstance(
                    ModEffects.stun(), FREEZE_TICKS, Stun.TIER_TWO, false, false, true));
        }
        Heaven.pull(body, player.position());
        if (player.getRandom().nextFloat() < GROUND_CHANCE) {
            rewriteGround(player, level, body.blockPosition().below());
        }

        Vec3 at = body.position().add(0.0, body.getBbHeight() * 0.5, 0.0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 24, 0.4, 0.5, 0.4, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, body.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                0.9F, 0.6F);
    }

    private static void eraseProjectiles(ServerPlayer player, ServerLevel level, Vec3 centre) {
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
                new AABB(centre, centre).inflate(PROJECTILE_RADIUS),
                projectile -> projectile.isAlive() && projectile.getOwner() != player
                        && !(projectile.getOwner() instanceof StandEntity stand && stand.getOwner() == player))) {
            Vec3 at = projectile.position();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.05);
            projectile.discard();
        }
    }

    private static void lockItems(Player victim) {
        ItemStack main = victim.getMainHandItem();
        ItemStack off = victim.getOffhandItem();
        if (!main.isEmpty()) {
            victim.getCooldowns().addCooldown(main.getItem(), ITEM_LOCK_TICKS);
        }
        if (!off.isEmpty()) {
            victim.getCooldowns().addCooldown(off.getItem(), ITEM_LOCK_TICKS);
        }
        if (victim.isBlocking()) {
            victim.stopUsingItem();
        }
    }

    private static void rewriteGround(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!BarrageDemolition.breakable(level, player, pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        Block altered = ALTERED.get(state.getBlock());
        if (altered != null) {
            level.setBlock(pos, altered.defaultBlockState(), Block.UPDATE_ALL);
            level.levelEvent(2001, pos, Block.getId(state));
            return;
        }
        level.destroyBlock(pos, true, player);
    }
}
