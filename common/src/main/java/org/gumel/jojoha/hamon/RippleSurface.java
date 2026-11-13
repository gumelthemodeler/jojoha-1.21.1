package org.gumel.jojoha.hamon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class RippleSurface {
    private static final int PROBE_DEPTH = 2;

    private static final double SKIM = 0.02;

    private RippleSurface() {
    }

    public static double surfaceAt(Level level, double x, double y, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int top = Mth_floor(y);

        for (int step = 0; step <= PROBE_DEPTH; step++) {
            pos.set(Mth_floor(x), top - step, Mth_floor(z));
            FluidState fluid = level.getFluidState(pos);
            if (fluid.isEmpty()) {
                continue;
            }
            return pos.getY() + fluid.getHeight(level, pos) + SKIM;
        }
        return Double.NaN;
    }

    public static boolean overWater(Level level, Vec3 at) {
        return !Double.isNaN(surfaceAt(level, at.x, at.y, at.z));
    }

    private static int Mth_floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
