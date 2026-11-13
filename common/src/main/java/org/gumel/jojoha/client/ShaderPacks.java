package org.gumel.jojoha.client;

import java.lang.reflect.Method;

public final class ShaderPacks {
    private static final String API = "net.irisshaders.iris.api.v0.IrisApi";

    private static boolean looked;
    private static Object api;
    private static Method inUse;

    private ShaderPacks() {
    }

    public static boolean active() {
        if (!looked) {
            looked = true;
            try {
                Class<?> type = Class.forName(API);
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | LinkageError missing) {
                api = null;
                inUse = null;
            }
        }
        if (inUse == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(inUse.invoke(api));
        } catch (ReflectiveOperationException failed) {
            return false;
        }
    }
}
