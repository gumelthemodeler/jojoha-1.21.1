package org.gumel.jojoha.block;

import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * Thin subclasses for the vanilla building blocks whose constructors are protected.
 *
 * <p>Five of the nine shapes in a building set - stairs, doors, trapdoors, buttons and pressure
 * plates - cannot be instantiated directly, so a subclass is the only way to make one. None of them
 * adds behaviour, and none of them should: the whole point is that a phantom staircase behaves
 * exactly like an oak one. They are gathered in a single file because each is three lines and
 * scattering them across five would be filing for its own sake.
 */
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
