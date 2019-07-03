package com.mqtt.sdk.listener;

public interface PushInitListener {

    void subscribeTopic(String topic);
    void pushDeviceInfo();
    void unregister();

    boolean isConnected();
}
