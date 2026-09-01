package org.gumel.jojoha.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import org.gumel.jojoha.Jojoha;

/**
 * Data components the mod puts on item stacks.
 *
 * <h2>The outfit</h2>
 *
 * <p>Armour that has been given an outfit is still the same armour: same item, same durability, same
 * enchantments, same protection. Only its appearance changes. That is exactly what a component is
 * for - it rides on the stack rather than turning the piece into a different item, which is what
 * keeps a worn, enchanted chestplate worn and enchanted after it has been dressed.
 *
 * <p>The value is a plain {@link ResourceLocation} naming the outfit, not a model or a texture.
 * Those are client concerns, and a server that has never heard of rendering still has to be able to
 * hold and save the value.
 */
public final class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Jojoha.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    /** Which outfit this piece wears, if any. Absent means the armour's own look. */
    public static final RegistrySupplier<DataComponentType<ResourceLocation>> OUTFIT =
            COMPONENTS.register("outfit", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private ModComponents() {
    }

    public static void bootstrap() {
    }
}
