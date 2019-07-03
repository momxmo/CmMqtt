package com.mqtt.sdk;

import com.aliyun.alink.linksdk.tools.AError;

public interface CmILinkKitConnectListener {
    void onError(AError var1);

    void onInitDone(Object var1);
}
