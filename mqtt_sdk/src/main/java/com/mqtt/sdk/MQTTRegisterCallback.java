package com.mqtt.sdk;

public interface MQTTRegisterCallback {
    /**
     * 注册成功回调
     * @param clientId 设备Id
     */
    void onSuccess(String clientId);

    /**
     * 注册失败回调
     * @param e 异常
     * @param message 异常信息
     */
    void onFailure(Exception e, String message);
    /**
     * 掉线回调
     * @param cause 异常
     */
    void connectionLost(Throwable cause);
    /**
     * 重连成功
     */
    void reconnectComplete();
}
