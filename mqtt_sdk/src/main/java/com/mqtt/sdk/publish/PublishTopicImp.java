package com.mqtt.sdk.publish;

import android.support.annotation.NonNull;

import com.mqtt.sdk.exception.PushException;
import com.mqtt.sdk.listener.PushCallback;
import com.mqtt.sdk.topic.Topics;

public class PublishTopicImp implements PublishTopic{
    public void publishMessage(@NonNull Topics topic, @NonNull String msg, @NonNull final PushCallback callback){
         if (topic == null) {
              if (callback != null)
                   callback.onFailure(new PushException("TopicType  is must"), "TopicType  is must");
              return;
         }
         if (msg == null) {
              if (callback != null)
                   callback.onFailure(new PushException("msg is must"), "msg is must");
              return;
         }
    }
}
