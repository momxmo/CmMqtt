package com.mqtt.sdk.topic;

public enum Topics {
    message,//消息接收
    update,//心跳时间间隔更新 （毫秒）
    cmd,//指令 log unbind upgrade ad
    uploadInfo,//上传设备信息
    playFeedback//播报完结 反馈服务端
}
