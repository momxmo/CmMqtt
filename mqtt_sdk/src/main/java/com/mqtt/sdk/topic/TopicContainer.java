package com.mqtt.sdk.topic;

import java.util.HashMap;
import java.util.Map;

public class TopicContainer {
    private Map<Topics, String> topicMap = new HashMap<>();
    private static TopicContainer mInstance = null;

    private TopicContainer() {
    }

    public static TopicContainer getInstance() {
        if (mInstance == null) {
            synchronized (TopicContainer.class) {
                if (mInstance == null) {
                    mInstance = new TopicContainer();
                }
            }
        }
        return mInstance;
    }


    public void setTopic(Topics topic, String topicStr) {
        topicMap.put(topic, topicStr);
    }

    public String getTopic(Topics topic) {
        return topicMap.get(topic);
    }

    public Topics getTopicType(String topic) {
        for (Map.Entry<Topics, String> entry : topicMap.entrySet()) {
            if (entry.getValue().equals(topic)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public boolean containsTopic(String topics) {
        for (Map.Entry<Topics, String> entry : topicMap.entrySet()) {
            if (entry.getValue().equals(topics)) {
                return true;
            }
        }
        return false;
    }
    public boolean containsTopic(Topics topics) {
        return topicMap.containsKey(topics);
    }

    public void removeTopic(Topics topics) {
        topicMap.remove(topics);
    }

    public void clear() {
        topicMap.clear();
    }

    public Map<Topics, String> getTopicMap() {
        return topicMap;
    }
}
