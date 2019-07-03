package com.mqtt.sdk.listener;

public interface PushCallback {

    /**
     * @param messageId 消息ID
     */
    void onSuccess(String messageId);

    /**
     * @param e 异常
     * @param message 异常信息
     */
    void onFailure(Exception e, String message);
}