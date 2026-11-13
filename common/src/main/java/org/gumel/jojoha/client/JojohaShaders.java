package org.gumel.jojoha.client;

import org.gumel.jojoha.Jojoha;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.architectury.event.events.client.ClientReloadShadersEvent;
import net.minecraft.client.renderer.ShaderInstance;

public final class JojohaShaders {

    private static final String TIME_STOP = "jojoha_timestop";
    private static final String IMPACT = "jojoha_impact";
    private static final String ERASURE = "jojoha_erasure";
    private static final String PALETTE = "jojoha_palette";
    private static final String SERVANT = "jojoha_servant";

    private static ShaderInstance timeStop;
    private static ShaderInstance impact;
    private static ShaderInstance erasure;
    private static ShaderInstance palette;
    private static ShaderInstance servant;

    private JojohaShaders() {
    }

    public static ShaderInstance servant() {
        return servant;
    }

    public static ShaderInstance palette() {
        return palette;
    }

    public static ShaderInstance timeStop() {
        return timeStop;
    }

    public static ShaderInstance impact() {
        return impact;
    }

    public static ShaderInstance erasure() {
        return erasure;
    }

    public static void register() {
        ClientReloadShadersEvent.EVENT.register((provider, sink) -> {
            try {
                sink.registerShader(
                        new ShaderInstance(provider, TIME_STOP, DefaultVertexFormat.POSITION),
                        shader -> timeStop = shader);
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not load the {} shader", TIME_STOP, failure);
                timeStop = null;
            }

            try {
                sink.registerShader(
                        new ShaderInstance(provider, IMPACT, DefaultVertexFormat.POSITION),
                        shader -> impact = shader);
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not load the {} shader", IMPACT, failure);
                impact = null;
            }

            try {
                sink.registerShader(
                        new ShaderInstance(provider, ERASURE, DefaultVertexFormat.POSITION),
                        shader -> erasure = shader);
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not load the {} shader", ERASURE, failure);
                erasure = null;
            }

            try {
                sink.registerShader(
                        new ShaderInstance(provider, PALETTE, DefaultVertexFormat.POSITION),
                        shader -> palette = shader);
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not load the {} shader", PALETTE, failure);
                palette = null;
            }

            try {
                sink.registerShader(
                        new ShaderInstance(provider, SERVANT, DefaultVertexFormat.NEW_ENTITY),
                        shader -> servant = shader);
            } catch (Exception failure) {
                Jojoha.LOGGER.error("Could not load the {} shader", SERVANT, failure);
                servant = null;
            }
        });
    }
}
