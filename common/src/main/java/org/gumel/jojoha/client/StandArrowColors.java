package org.gumel.jojoha.client;

import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import net.minecraft.client.Minecraft;
import org.gumel.jojoha.registry.ModItems;

/**
 * Runs the Stand Arrow's texture through the colour wheel while a ritual is under way.
 *
 * <p>This is the recolour; {@code ItemRendererMixin} adds the light on top of it. They are separate
 * on purpose, because they do different jobs and neither does both: a tint multiplies the arrow's
 * own texture, which changes what colour the arrow <em>is</em> but can only ever darken it, while
 * an additive pass adds light without ever changing the underlying material. Together they read as
 * an arrow that has gone rainbow and is throwing the colour off itself.
 *
 * <p>It also goes through vanilla's own item colour pipeline rather than any rendering of ours,
 * so it applies anywhere the item is drawn - in hand, on the ground, in a GUI.
 *
 * <p><strong>Currently parked - nothing calls into this.</strong> To switch the effect back on:
 * <ol>
 *   <li>add {@code "client.ItemRendererMixin"} back to the client list in
 *       {@code jojoha.mixins.json} - that is the additive light pass;</li>
 *   <li>call {@code StandArrowColors.register()} from {@code JojohaFabricClient.onInitializeClient}
 *       and from {@code JojohaNeoForgeClientModBus.onClientSetup} - that is the recolour;</li>
 *   <li>restore the {@code StandArrowGlow} begin/tick/clear calls in {@code StandRitualEffects},
 *       which are what decide when any of it is running.</li>
 * </ol>
 *
 * <p>The arrow model keeps its {@code "tintindex": 0} faces in the meantime. That costs nothing
 * while parked - with no colour handler registered, {@code ItemColors.getColor} returns -1, an
 * opaque white that multiplies the texture by itself - and it is the fiddly half of the setup to
 * redo, being 192 faces of generated model JSON.
 */
public final class StandArrowColors {
    private StandArrowColors() {
    }

    public static void register() {
        // tintIndex 0 is the only one the model declares - see stand_arrow.json, where every face
        // carries it. A face with no tint index is never passed through here at all.
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> {
            if (tintIndex != 0 || !StandArrowGlow.isAnyActive()) {
                return StandArrowGlow.NO_TINT;
            }

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null) {
                return StandArrowGlow.NO_TINT;
            }

            float clientTime = (float) minecraft.level.getGameTime()
                    + minecraft.getTimer().getGameTimeDeltaPartialTick(false);
            return StandArrowGlow.rainbowTint(clientTime);
        }, ModItems.STAND_ARROW);
    }
}
