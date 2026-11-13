package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.registry.ModSounds;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.HierophantAim;
import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.hierophant.HierophantTraps;
import org.gumel.jojoha.stand.hierophant.Tendrils;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class MiniatureTrapSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "miniature_trap");
    public static final MiniatureTrapSkill INSTANCE = new MiniatureTrapSkill();

    private static final double REACH = 6.0;
    private static final double SPAN = 10.0;
    private static final int LASTS_TICKS = 60 * 20;

    private static final int COOLDOWN_TICKS = 60;
    private static final float ENERGY_COST = EnergyWeight.LIGHT.cost();

    private MiniatureTrapSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.miniature_trap";
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
        BlockHitResult near = HierophantAim.block(player, REACH);
        if (near.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3 out = Vec3.atLowerCornerOf(near.getDirection().getNormal());
        Vec3 from = near.getLocation().add(out.scale(0.02));
        BlockHitResult far = level.clip(new ClipContext(from.add(out.scale(0.1)), from.add(out.scale(SPAN)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (far.getType() == HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.jojoha.miniature_trap.nothing_across"), true);
            return true;
        }
        Vec3 to = far.getLocation().subtract(out.scale(0.02));
        BlockPos at = near.getBlockPos();

        if (stand == null) {
            set(level, player, null, from, to, at);
            return true;
        }
        stand.triggerStrikeForward(HierophantClips.MINE);
        StandBeat.at(HierophantClips.MINE_CONTACT_TICKS, player,
                () -> set(level, player, StandEntity.findFor(player), from, to, at));
        return true;
    }

    private static void set(ServerLevel level, ServerPlayer player, StandEntity stand, Vec3 from, Vec3 to, BlockPos at) {
        HierophantTraps.wire(level, player, from, to, LASTS_TICKS);
        Tendrils.line(level, Tendrils.newKey(), HierophantAim.legs(player, stand), from, Tendrils.THIN, 10,
                Tendrils.tintOf(player));
        level.playSound(null, at, ModSounds.HG_NET_SET.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
    }
}
