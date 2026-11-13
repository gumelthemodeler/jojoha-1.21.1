package com.github.exopandora.shouldersurfing.api.event;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeCameraCouplingEventHandler;

public interface IEventBus {
    void register(int priority, ComputeCameraCouplingEventHandler handler);
}
