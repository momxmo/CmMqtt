package com.mqtt.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.mqtt.sdk.model.Subscription;
import com.mqtt.sdk.tool.MQLog;
import com.mqtt.sdk.tool.SharedPreferenceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * 回调Mqtt消息
 */
public abstract class MQTTReceiver extends BroadcastReceiver {

    protected Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        if (context != null && intent != null) {
            if (MQTTConstants.MQTT_RECEIVER.equals(intent.getAction())) {
                String clientid = intent.getStringExtra(MQTTConstants.CLIENTID);
                String topic = intent.getStringExtra(MQTTConstants.TOPIC);
                String message = intent.getStringExtra(MQTTConstants.MQTTMESSAGE);
                if (TextUtils.isEmpty(topic) || TextUtils.isEmpty(message)) {
                    return;
                }
                internalHandler(clientid, topic, message);
            }
        }
    }


    private void internalHandler(String clientid, String topic, String message) {
        MQLog.i("topic : " + topic + " message" + message);
        //消息过滤，只接受自己订阅的消息
//        Connection connection = MqttManager.getInstance().getConnection();
//        boolean subscriptionsFilter = connection.isSubscriptionsFilter(topic);
//        if (!subscriptionsFilter) {
//            return;
//        }
        if (topic.equals(MQTTConstants.IOT_MESSAGE + clientid)) {
            handlerMessage(topic, message);
        } else if (topic.equals(MQTTConstants.IOT_UPDATE + clientid)) {
            try {
                JSONObject jsonMessage = new JSONObject(message);
                if (jsonMessage.has("pingInterval")) {
                    updateKeepAlive(jsonMessage.getInt("pingInterval"));
                }
            } catch (JSONException e) {
                MQLog.e(e.getMessage());
            }
        } else if (topic.equals(MQTTConstants.IOT_CMD + clientid)) {
            try {
                JSONObject jsonMessage = new JSONObject(message);
                if (jsonMessage.has("cmd")) {
                    String cmd = jsonMessage.getString("cmd");
                    switch (cmd) {
                        case MQTTConstants.CMD_UNBIND_TPYE:
                            UnbindMessage();
                            break;
                        case MQTTConstants.CMD_LOG_TPYE:
                            LogMessage();
                            break;
                        case MQTTConstants.CMD_AD_TPYE:
                            AdMessage();
                            break;
                        case MQTTConstants.CMD_UPGRADE_TPYE: {
                            UpdateMessage(message);
                        }
                        break;
                    }
                }
            } catch (JSONException e) {
                MQLog.e(e.getMessage());
            }
        } else {
            handlerMessage(topic, message);
        }
    }

    /**
     * 更新心跳包时间
     *
     * @param keepAliveTime
     */
    private void updateKeepAlive(int keepAliveTime) {
        if (keepAliveTime >= 500) {//可能是毫秒
            keepAliveTime = keepAliveTime / 1000;
        }
        SharedPreferenceUtil.getInstance(context).putInt(SharedPreferenceUtil.KEEP_ALIVE, keepAliveTime);
        MqttManager.getInstance().updateKeepAlive(keepAliveTime);
    }

    /**
     * 消息
     *
     * @param topic
     * @param message
     */
    public abstract void handlerMessage(String topic, String message);

    public abstract void LogMessage();

    public abstract void UnbindMessage();

    public abstract void AdMessage();

    public abstract void UpdateMessage(String upgradeUrl);
}
