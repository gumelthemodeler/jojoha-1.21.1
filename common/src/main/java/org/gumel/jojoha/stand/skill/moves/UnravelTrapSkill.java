package org.gumel.jojoha.stand.skill.moves;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.gumel.jojoha.Jojoha;
import org.gumel.jojoha.data.JojohaPlayerData;
import org.gumel.jojoha.stand.StandEntity;
import org.gumel.jojoha.stand.hierophant.HierophantAim;
import org.gumel.jojoha.stand.hierophant.HierophantClips;
import org.gumel.jojoha.stand.hierophant.HierophantTraps;
import org.gumel.jojoha.stand.hierophant.Tendrils;
import org.gumel.jojoha.stand.skill.EnergyWeight;
import org.gumel.jojoha.stand.skill.StandBeat;
import org.gumel.jojoha.stand.skill.StandSkill;

public final class UnravelTrapSkill implements StandSkill {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Jojoha.MOD_ID, "unravel_trap");
    public static final UnravelTrapSkill INSTANCE = new UnravelTrapSkill();

    private static final double REACH = 20.0;
    private static final double RADIUS = 3.0;
    private static final int LASTS_TICKS = 30 * 20;
    private static final int FLOOR_SEARCH = 4;

    private static final int COOLDOWN_TICKS = 300;
    private static final float ENERGY_COST = EnergyWeight.STANDARD.cost();

    private UnravelTrapSkill() {
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public String translationKey() {
        return "skill.jojoha.unravel_trap";
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
        BlockHitResult hit = HierophantAim.block(player, REACH);
        if (hit.getType() == HitResult.Type.MISS) {
            return false;
        }
        Vec3 floor = floor(level, hit);
        if (floor == null) {
            return false;
        }

        if (stand == null) {
            set(level, player, null, floor);
            return true;
        }
        stand.triggerStrikeForward(HierophantClips.MINE);
        StandBeat.at(HierophantClips.MINE_CONTACT_TICKS, player, () -> set(level, player, StandEntity.findFor(player), floor));
        return true;
    }

    private static void set(ServerLevel level, ServerPlayer player, StandEntity stand, Vec3 floor) {
        HierophantTraps.web(level, player, floor, RADIUS, LASTS_TICKS);

        Vec3 legs = HierophantAim.legs(player, stand);
        int tint = Tendrils.tintOf(player);
        var random = level.getRandom();
        for (int i = 0; i < 5; i++) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = RADIUS * (0.4 + random.nextDouble() * 0.6);
            Vec3 foot = floor.add(Math.cos(a) * r, 0.04, Math.sin(a) * r);
            Tendrils.line(level, Tendrils.newKey(), legs, foot, Tendrils.THIN, 24, tint);
        }
        level.playSound(null, BlockPos.containing(floor), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    private static Vec3 floor(ServerLevel level, BlockHitResult hit) {
        if (hit.getDirection() == Direction.UP) {
            return hit.getLocation();
        }
        BlockPos start = hit.getBlockPos().relative(hit.getDirection());
        for (int down = 0; down <= FLOOR_SEARCH; down++) {
            BlockPos below = start.below(down + 1);
            if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                    && level.getBlockState(below.above()).getCollisionShape(level, below.above()).isEmpty()) {
                return new Vec3(hit.getLocation().x, below.getY() + 1.0, hit.getLocation().z);
            }
        }
        return null;
    }
}
