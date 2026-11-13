package org.gumel.jojoha.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

public final class PhantomBuilding {

    private PhantomBuilding() {
    }

    public static class Stairs extends StairBlock {
        public Stairs(BlockState base, Properties properties) {
            super(base, properties);
        }
    }

    public static class Door extends DoorBlock {
        public Door(BlockSetType type, Properties properties) {
            super(type, properties);
        }
    }

    public static class Trapdoor extends TrapDoorBlock {
        public Trapdoor(BlockSetType type, Properties properties) {
            super(type, properties);
        }
    }

    public static class Button extends ButtonBlock {
        public Button(BlockSetType type, int ticksPressed, Properties properties) {
            super(type, ticksPressed, properties);
        }
    }

    public static class Plate extends PressurePlateBlock {
        public Plate(BlockSetType type, Properties properties) {
            super(type, properties);
        }
    }
}
