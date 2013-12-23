package com.tizen.tizenrunner.api;

/**
 * Interface between the StubManager API and the TizenRunner that communicates with GASClient.
 * @author Bao Hongbin
 */
public interface IDeviceStub {
    IDevice waitForConnection();
    IDevice waitForConnection(long timeout, String serial);
    void shutdown();
}
