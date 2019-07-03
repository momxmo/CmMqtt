package com.mqtt.sdk.publish;

import android.support.annotation.NonNull;

import com.mqtt.sdk.listener.PushCallback;
import com.mqtt.sdk.topic.Topics;

public interface PublishTopic {
     void publishMessage(@NonNull Topics topic, @NonNull String msg, @NonNull final PushCallback callback);
}
