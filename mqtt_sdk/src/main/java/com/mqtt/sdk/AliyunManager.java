package com.mqtt.sdk;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.aliyun.alink.dm.api.DMErrorCode;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.linkkit.api.ILinkKitConnectListener;
import com.aliyun.alink.linkkit.api.IoTMqttClientConfig;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linkkit.api.LinkKitInitParams;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.tmp.device.payload.ValueWrapper;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;
import com.mqtt.sdk.exception.PushException;
import com.mqtt.sdk.listener.PushCallback;
import com.mqtt.sdk.publish.PublishTopic;
import com.mqtt.sdk.topic.TopicBean;
import com.mqtt.sdk.topic.TopicContainer;
import com.mqtt.sdk.topic.Topics;

import java.util.HashMap;
import java.util.Map;


public class AliyunManager extends BasePushManager implements PublishTopic {
    private String TAG = "AliyunManager";
    private static AliyunManager mInstance = null;
    private CmILinkKitConnectListener mCMILinkKitConnectListener;
    private DeviceInfo deviceInfo;
    private ConnectState connectState;

    private AliyunManager() {
    }

    public static AliyunManager getInstance() {
        if (mInstance == null) {
            synchronized (AliyunManager.class) {
                if (mInstance == null) {
                    mInstance = new AliyunManager();
                }
            }
        }
        return mInstance;
    }

    @Override
    public void registerAliyun(Context context, String productKey, String deviceName, String deviceSecret, TopicBean topicBean, CmILinkKitConnectListener mCMILinkKitConnectListener) {
        this.context = context;
        this.mCMILinkKitConnectListener = mCMILinkKitConnectListener;
        if (TextUtils.isEmpty(productKey) || TextUtils.isEmpty(deviceName) || TextUtils.isEmpty(deviceSecret)) {
            if (mCMILinkKitConnectListener != null) {
                mCMILinkKitConnectListener.onError(DMErrorCode.getErrorCode(1101101, 121, "init params error, params is null."));
            }
            return;
        }
        if (topicBean == null) {
            if (mCMILinkKitConnectListener != null) {
                mCMILinkKitConnectListener.onError(DMErrorCode.getErrorCode(1101101, 121, "topicBean params is null."));
            }
            return;
        }
        this.deviceInfo = new DeviceInfo();
        this.deviceName = deviceName;
        this.productKey = productKey;
        deviceInfo.productKey = productKey;
        deviceInfo.deviceName = deviceName;
        deviceInfo.deviceSecret = deviceSecret;
        //TODO 處理{productKey ,deviceName}通配符
        topicBean = handlerTopic(productKey, deviceName, topicBean);
        TopicContainer.getInstance().setTopic(Topics.message, topicBean.getMessageTopic());
        TopicContainer.getInstance().setTopic(Topics.cmd, topicBean.getCmdTopic());
        TopicContainer.getInstance().setTopic(Topics.update, topicBean.getUpdateTopic());
        TopicContainer.getInstance().setTopic(Topics.uploadInfo, topicBean.getUploadInfoTopic());
        TopicContainer.getInstance().setTopic(Topics.playFeedback, topicBean.getPlayFeedbackTopic());
        LinkKit.getInstance().registerOnPushListener(notifyListener);
        initAliyun();
        simListener(context);
    }

    private TopicBean handlerTopic(String productKey, String deviceName, TopicBean topicBean) {
        if (topicBean.getMessageTopic() != null) {
            String topic = topicBean.getMessageTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topicBean.setMessageTopic(topic);
        }
        if (topicBean.getCmdTopic() != null) {
            String topic = topicBean.getCmdTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topicBean.setCmdTopic(topic);
        }
        if (topicBean.getUpdateTopic() != null) {
            String topic = topicBean.getUpdateTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topicBean.setUpdateTopic(topic);
        }
        if (topicBean.getUploadInfoTopic() != null) {
            String topic = topicBean.getUploadInfoTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topicBean.setUploadInfoTopic(topic);
        }
        if (topicBean.getPlayFeedbackTopic() != null) {
            String topic = topicBean.getPlayFeedbackTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topicBean.setPlayFeedbackTopic(topic);
        }
        return topicBean;
    }

    @Override
    public void unregister() {
        TopicContainer.getInstance().clear();
        LinkKit.getInstance().unRegisterOnPushListener(notifyListener);
        LinkKit.getInstance().deinit();
    }

    /**
     * 持久化设备的三元组信息，用于后续的连接
     */
    private void initAliyun() {
        /**
         * 设置设备当前的初始状态值，属性需要和云端创建的物模型属性一致
         * 如果这里什么属性都不填，物模型就没有当前设备相关属性的初始值。
         * 用户调用物模型上报接口之后，物模型会有相关数据缓存。
         */
        Map<String, ValueWrapper> propertyValues = new HashMap<>();
        IoTMqttClientConfig clientConfig = new IoTMqttClientConfig(deviceInfo.productKey, deviceInfo.deviceName, deviceInfo.deviceSecret);
        LinkKitInitParams params = new LinkKitInitParams();
        params.deviceInfo = deviceInfo;
        params.propertyValues = propertyValues;
        params.mqttClientConfig = clientConfig;
        /**
         * 设备初始化建联
         * onError 初始化建联失败，需要用户重试初始化。如因网络问题导致初始化失败。
         * onInitDone 初始化成功
         */
        LinkKit.getInstance().init(context, params, new ILinkKitConnectListener() {
            @Override
            public void onError(AError error) {
                // 初始化失败 error包含初始化错误信息
                if (mCMILinkKitConnectListener != null) {
                    mCMILinkKitConnectListener.onError(error);
                }
            }

            @Override
            public void onInitDone(Object data) {
                ALog.i(TAG, "Aliyun 1111111111111111111111");
                // 初始化成功 data 作为预留参数
                if (mCMILinkKitConnectListener != null) {
                    mCMILinkKitConnectListener.onInitDone(data);
                }
            }
        });
    }

    private IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        @Override
        public void onNotify(String connectId, String topic, AMessage aMessage) {
            String payload = new String((byte[]) aMessage.data);
            ALog.i(TAG, "Aliyun connectId =" + connectId + "  topic=" + topic + "   pushData=" + payload);
            if (TopicContainer.getInstance().containsTopic(topic)) {
                Intent intent = new Intent();
                intent.setAction(MQTTConstants.MQTT_RECEIVER);
                intent.putExtra(MQTTConstants.TOPIC, topic);
                intent.putExtra(MQTTConstants.CLIENTID, connectId);
                intent.putExtra(MQTTConstants.MQTTMESSAGE, payload);
                context.sendBroadcast(intent);
            }
        }

        @Override
        public boolean shouldHandle(String connectId, String topic) {
            return true;
        }

        @Override
        public void onConnectStateChange(String connectId, ConnectState connectState) {
            // 对应连接类型的连接状态变化回调，具体连接状态参考 SDK ConnectState
            AliyunManager.this.connectState = connectState;
            if (connectState == ConnectState.CONNECTED) {
                ALog.i(TAG, "Aliyun 22222222222222222");
                connectedHandler();
            }
        }
    };

    /**
     * 订阅
     */
    @Override
    public void subscribeTopic(String topic) {
        try {
            MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
            subscribeRequest.isSubscribe = false;
            subscribeRequest.topic = topic;
            LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
                @Override
                public void onSuccess() {
                    ALog.i(TAG, "订阅成功");
                }

                @Override
                public void onFailure(AError aError) {
                    ALog.i(TAG, "订阅失败 " + aError.getMsg() + "  " + aError.getCode() + "" + aError.getDomain());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void publishMessage(@NonNull Topics topics, @NonNull String msg, @NonNull final PushCallback callback) {
        try {
            if (topics == null) {
                if (callback != null)
                    callback.onFailure(new PushException("TopicType  is must"), "TopicType  is must");
                return;
            }
            if (msg == null) {
                if (callback != null)
                    callback.onFailure(new PushException("msg is must"), "msg is must");
                return;
            }

            MqttPublishRequest request = new MqttPublishRequest();
            request.qos = 1;
            request.isRPC = false;
            request.topic = TopicContainer.getInstance().getTopic(topics);
            if (TextUtils.isEmpty(request.topic)) {
                if (callback != null) {
                    callback.onFailure(new PushException("not fond Topic type"), "not fond Topic type");
                }
                return;
            }
            request.msgId = String.valueOf(IDGenerater.generateId());
            request.payloadObj = msg;
            LinkKit.getInstance().publish(request, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    ALog.i(TAG, "阿里推送成功" + aResponse.data);
                    if (callback != null && aRequest != null && aRequest instanceof MqttPublishRequest) {
                        callback.onSuccess(((MqttPublishRequest) aRequest).msgId);
                    }
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    ALog.i(TAG, "阿里推送失败" + aError.getMsg());
                    if (callback != null) {
                        if (aError == null) {
                            callback.onFailure(new PushException("发送消息失败"), "发送消息失败");
                        } else {
                            callback.onFailure(new PushException(aError.getCode() + ""), aError.getMsg());
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(new PushException(e), e.getMessage());
            }
        }
    }

    public boolean isConnected() {
        if (connectState != null && connectState == ConnectState.CONNECTED) {
            return true;
        }
        return false;
    }

    public void setDebug(boolean debug) {
        if(debug){
            ALog.setLevel(ALog.LEVEL_DEBUG);//开启Debug
        }
    }
}
