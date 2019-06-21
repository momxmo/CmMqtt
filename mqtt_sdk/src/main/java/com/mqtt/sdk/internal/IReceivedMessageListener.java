package com.mqtt.sdk.internal;


import com.mqtt.sdk.model.ReceivedMessage;

public interface IReceivedMessageListener {

    void onMessageReceived(ReceivedMessage message);
}