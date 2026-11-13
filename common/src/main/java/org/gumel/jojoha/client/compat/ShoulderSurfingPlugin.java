package org.gumel.jojoha.client.compat;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeCameraCouplingEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import org.gumel.jojoha.client.LockOn;

public final class ShoulderSurfingPlugin implements IShoulderSurfingPlugin {
    private static final int AFTER_BUILTIN = 2000;

    @Override
    public void register(IEventBus eventBus) {
        eventBus.register(AFTER_BUILTIN, (ComputeCameraCouplingEventHandler) event -> {
            if (LockOn.active()) {
                event.setResult(true);
            }
        });
    }
}
