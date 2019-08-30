package com.mqtt.sdk;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.mqtt.sdk.listener.PushInitListener;
import com.mqtt.sdk.publish.PublishTopic;
import com.mqtt.sdk.tool.MQLog;
import com.mqtt.sdk.tool.MQTools;
import com.mqtt.sdk.topic.TopicBean;
import com.mqtt.sdk.topic.TopicContainer;
import com.mqtt.sdk.topic.Topics;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class BasePushManager implements PublishTopic, PushInitListener {
    protected Context context;
    protected String deviceName;
    protected String productKey;//阿里使用到
    protected SimPhoneStateListener myphonelister;
    protected int simSignal = -1;//sim卡信号强度F
    protected TelephonyManager Tel;

    public class SimPhoneStateListener extends PhoneStateListener {
        private Context context;

        public SimPhoneStateListener(Context context) {
            this.context = context;
        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            simSignal = signalStrength.getGsmSignalStrength();  //获取信号强度
        }
    }

    public void simListener(Context context) {
        if (myphonelister == null) {
            myphonelister = new SimPhoneStateListener(context);
            Tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Tel != null) {
                Tel.listen(myphonelister, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }

    public void registerQMTT(@NonNull Context mContext, String productKey, String serverUrl, String sn, String token, TopicBean topicBean, @Nullable final MQTTRegisterCallback mCallback) {
        this.context = mContext;
        this.productKey = productKey;
        this.deviceName = sn;
    }

    public void registerAliyun(Context context, String productKey, String deviceName, String deviceSecret, TopicBean topicBean, MQTTRegisterCallback mMQTTRegisterCallback) {
    }

    public void connectedHandler() {
        subscribeAllTopics();
        pushDeviceInfo();
    }

    /**
     * 订阅
     */
    protected abstract void subscribeAllTopics();

    @Override
    public void pushDeviceInfo() {
        if (context != null) {
            try {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", deviceName);
                map.put("connected_type", MQTools.getNetWorkConnectionType(context) + "");
                map.put("imei", MQTools.getIMEI(context));
                if (MQTools.hasSimCard(context)) {
                    map.put("sim_type", MQTools.getNetworkOperatorNo(context) + "");
                    map.put("sim_signal_strength", simSignal + "");
                    map.put("sim_number", MQTools.getSimNumber(context) + "");
                    map.put("iccid", MQTools.getICCID(context) + "");
                }
                map.put("wifi_strength", MQTools.obtainWifiStrength(context) + "");
                map.put("mac_address", MQTools.getMac(context));
                map.put("app_version", MQTools.getVersionName(context));
                map.put("os_version", Build.VERSION.RELEASE);
                map.put("push_version", MQTTConstants.MQTT_ANDROID_SDK);
                //硬件版本  map.put("hardware_version", Build.VERSION.RELEASE);
                map.put("boot_version", android.os.Build.BOOTLOADER);
                map.put("model", Build.MODEL);
                map.put("current_volume", MQTools.getSystemVolume(context) + "");
                map.put("battery_level", MQTools.getSystemBattery(context) + "");
                map.put("packgeName", context.getPackageName());
                JSONObject json = new JSONObject(map);
                String message = json.toString();
                MQLog.i(message);
                publishMessage(Topics.uploadInfo, message, null);
            } catch (Exception e) {
                MQLog.e(e.getMessage());
            }
        }
    }
}
