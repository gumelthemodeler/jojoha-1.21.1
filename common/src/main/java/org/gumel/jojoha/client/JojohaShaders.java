package org.gumel.jojoha.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.architectury.event.events.client.ClientReloadShadersEvent;
import net.minecraft.client.renderer.ShaderInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's own shader programs, as opposed to the vanilla ones it used to replace.
 *
 * <p>Registered through Architectury's reload event, which is the one piece of this that genuinely
 * differs per loader and the one piece we therefore do not write twice.
 *
 * <h2>Why the name is prefixed rather than namespaced</h2>
 *
 * <p>The obvious spelling is {@code jojoha:timestop}, and it does not work. Vanilla's
 * {@code ShaderInstance} constructor resolves the name through
 * {@code ResourceLocation.withDefaultNamespace}, which forces {@code minecraft} and would then be
 * handed a path containing a colon - not a valid path, so it throws. Both loaders patch that
 * constructor to accept a {@code ResourceLocation}, but each with their own overload, and using
 * either would put a loader-specific call in common code to solve a naming problem.
 *
 * <p>So the program lives in the {@code minecraft} namespace under a prefixed name. Nothing is
 * overridden by doing that - {@code jojoha_timestop} is a file vanilla does not have - and the
 * awkwardness is confined to this comment.
 */
public final class JojohaShaders {
    private static final Logger LOGGER = LoggerFactory.getLogger("jojoha-shaders");

    /** The names of the programs, and of the three files each of them is made of. */
    private static final String TIME_STOP = "jojoha_timestop";
    private static final String IMPACT = "jojoha_impact";

    private static ShaderInstance timeStop;
    private static ShaderInstance impact;

    private JojohaShaders() {
    }

    /** The time stop pass, or null before the first resource load has finished. */
    public static ShaderInstance timeStop() {
        return timeStop;
    }

    /** The black and white impact pass, or null before the first resource load has finished. */
    public static ShaderInstance impact() {
        return impact;
    }

    public static void register() {
        ClientReloadShadersEvent.EVENT.register((provider, sink) -> {
            try {
                sink.registerShader(
                        new ShaderInstance(provider, TIME_STOP, DefaultVertexFormat.POSITION),
                        shader -> timeStop = shader);
            } catch (Exception failure) {
                // Logged rather than thrown. A shader that will not compile should cost the mod its
                // time stop visuals, not the client its startup - and the message is the only way
                // anyone finds out which line of GLSL was wrong.
                LOGGER.error("[jojoha] Could not load the {} shader", TIME_STOP, failure);
                timeStop = null;
            }

            try {
                sink.registerShader(
                        new ShaderInstance(provider, IMPACT, DefaultVertexFormat.POSITION),
                        shader -> impact = shader);
            } catch (Exception failure) {
                LOGGER.error("[jojoha] Could not load the {} shader", IMPACT, failure);
                impact = null;
            }
        });
    }
}
