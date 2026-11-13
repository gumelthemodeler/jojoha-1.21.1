package com.github.exopandora.shouldersurfing.api.client;

public interface IShoulderSurfing {
    IShoulderSurfingCamera getCamera();

    boolean isShoulderSurfing();

    static IShoulderSurfing getInstance() {
        throw new UnsupportedOperationException();
    }
}
