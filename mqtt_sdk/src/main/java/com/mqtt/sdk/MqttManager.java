package com.mqtt.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.mqtt.sdk.bean.TopicType;
import com.mqtt.sdk.exception.PushException;
import com.mqtt.sdk.exception.RegisterException;
import com.mqtt.sdk.listener.MqttTraceCallback;
import com.mqtt.sdk.listener.PushCallback;
import com.mqtt.sdk.model.ConnectionModel;
import com.mqtt.sdk.model.Subscription;
import com.mqtt.sdk.tool.MQLog;
import com.mqtt.sdk.tool.MQTools;
import com.mqtt.sdk.tool.SharedPreferenceUtil;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.internal.NetworkModuleService;
import org.json.JSONObject;

import java.io.EOFException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MqttManager {

    private static final String TAG = "MqttManager";
    private static MqttManager mInstance;
    private Context context;
    private MQTTRegisterCallback mMQTTRegisterCallback;
    private Connection connection;
    private SimPhoneStateListener myphonelister;
    private TelephonyManager Tel;
    private int simSignal = -1;//sim卡信号强度F

    private MqttManager() {
    }

    public static MqttManager getInstance() {
        if (mInstance == null) {
            synchronized (MqttManager.class) {
                if (mInstance == null) {
                    mInstance = new MqttManager();
                }
            }
        }
        return mInstance;
    }


    private MqttCallbackExtended mMqttCallbackExtended = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                if (mMQTTRegisterCallback != null) {
                    mMQTTRegisterCallback.reconnectComplete();
                }
                MQLog.d("connectComplete: reconnect success " + serverURI);
            } else {
                MQLog.d("connectComplete: " + serverURI);
            }
            if (connection != null) {
                connection.addAction("Connection connectComplete");
                connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTED);
            }
            connectedHandler();
        }


        @Override
        public void connectionLost(Throwable cause) {
            if (connection != null) {
                connection.addAction("Connection Lost");
                connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTED);
            }
            if (mMQTTRegisterCallback != null) {
                if (cause == null) {
                    mMQTTRegisterCallback.connectionLost(new MqttException(MqttException.REASON_CODE_CONNECTION_LOST, new Exception("connection lost")));
                } else {
                    mMQTTRegisterCallback.connectionLost(cause);
                }
            }
//            if (mMQTTRegisterCallback!=null && cause instanceof MqttException) {
//                MqttException cause1= (MqttException) cause;
//                if (cause1.getReasonCode() == MqttException.REASON_CODE_CONNECTION_LOST) {
//                    //被踢掉线
//                    mMQTTRegisterCallback.connectionLost(cause);
//                }
//            }
            MQLog.d("connectionLost: connection was lost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String payload = new String(message.getPayload());
            MQLog.d("Topic: " + topic + " isDuplicate: "+message.isDuplicate()+" messageId: "+message.getId()+"  ==> Payload: " + payload);
            if (topic != null && payload != null) {
                if (connection != null) {
                    if (connection.getSubscriptionsFilter(topic)) {
                        Intent intent = new Intent();
                        intent.setAction(MQTTConstants.MQTT_RECEIVER);
                        intent.putExtra(MQTTConstants.TOPIC, topic);
                        intent.putExtra(MQTTConstants.CLIENTID, connection.getId());
                        intent.putExtra(MQTTConstants.MQTTMESSAGE, payload);
                        context.sendBroadcast(intent);
                    } else {
                        MQLog.d("订阅以外的消息：Topic: " + topic + " ==> Payload: " + payload);
                        if (MQTTConstants.SYS_CLOSE.equals(topic)) {
                            MQLog.d("服务器主动断开：" + payload);
                        }
                    }
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //即服务器成功delivery消息 消息发送成功ack校验
        }
    };


    public void simListener(Context context) {
        if (myphonelister == null) {
            myphonelister = new SimPhoneStateListener(context);
            Tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (Tel != null) {
                Tel.listen(myphonelister, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }

    public void registerQMTT(@NonNull Context mContext, String serverUrl, String sn, String token, String[] topics, @Nullable final MQTTRegisterCallback mCallback) {
        if (mContext == null) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("context can not be empty "), "context can not be empty ");
            }
            return;
        }
        if (TextUtils.isEmpty(serverUrl)) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("url is necessary"), "url is necessary");
            }
            return;
        }
        if (TextUtils.isEmpty(sn)) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("sn is necessary"), "sn is necessary");
            }
            return;
        }
        if (TextUtils.isEmpty(token)) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("token is necessary"), "token is necessary");
            }
            return;
        }
        URI url;
        try {
            NetworkModuleService.validateURI(serverUrl);
            url = new URI(serverUrl);
        } catch (Exception e) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("URI path must be empty"), "URI path must be empty");
            }
            return;
        }
        try {
            if (topics != null && topics.length > 0) {
                for (int i = 0; i < topics.length; ++i) {
                    MqttTopic.validate(topics[i], true/* allow wildcards */);
                }
            }
        } catch (Exception e) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException(e), "The topic is Invalid");
            }
            return;
        }

        if (connection != null ) {
            ConnectionModel connectionModel = new ConnectionModel(connection);
            if (url.getHost().equals(connectionModel.getServerHostName())
                    && url.getPort() == connectionModel.getServerPort()
                    && sn.equals(connectionModel.getClientId())
                    && token.equals(connectionModel.getPassword())) {
                this.mMQTTRegisterCallback = mCallback;
                if (mMQTTRegisterCallback != null) {
                    mMQTTRegisterCallback.onSuccess(connection.getId());
                }
                if (topics != null && topics.length > 0 && connection.getClient() != null ) {
                    ArrayList<Subscription> newSubs = new ArrayList<>();
                    for (String topic : topics) {
                        newSubs.add(new Subscription(topic, 1, connection.handle(), false));
                        try {
                            if (connection.getClient().isConnected()) {
                                connection.getClient().subscribe(topic, 1);
                            }
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    connection.setSubscriptions(newSubs);
                }
                return;
            }
        }
        this.context = mContext.getApplicationContext();
        this.mMQTTRegisterCallback = mCallback;

        final ConnectionModel connectionModel = new ConnectionModel();
        connectionModel.setClientId(sn);
        connectionModel.setServerHostName(url.getHost());
        connectionModel.setServerPort(url.getPort());
        connectionModel.setCleanSession(true);
        connectionModel.setUsername(sn);
        connectionModel.setPassword(token);
//        connectionModel.setTlsServerKey("XXXXXXXXXXXXXX");
//        connectionModel.setTlsClientKey("XXXXXXXXXXXX");
        connectionModel.setTimeout(40);
        connectionModel.setKeepAlive(SharedPreferenceUtil.getInstance(context).getInt(SharedPreferenceUtil.KEEP_ALIVE, 30));
        connectionModel.setLwtRetain(true);
        connection(topics, connectionModel, false);
    }

    private synchronized void connection(String[] topics, ConnectionModel connectionModel, boolean isReset) {
        try {
            if (connection != null) {
                MqttAndroidClient client = connection.getClient();
                connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
                if(client!=null){
                    client.disconnect();
                }
                connection.updateConnection(connectionModel.getClientId(), connectionModel.getServerHostName(), connectionModel.getServerPort(), connectionModel.isTlsConnection());
            } else {
                connection = Connection.createConnection(
                        connectionModel.getClientHandle(), connectionModel.getClientId(),
                        connectionModel.getServerHostName(), connectionModel.getServerPort(),
                        context, connectionModel.isTlsConnection());
            }
            if (!isReset) {
                connection.clearSubscriptions();
                ArrayList<Subscription> defaultSubscription = setDefaultSubscription(topics);
                connection.setSubscriptions(defaultSubscription);
            }
            connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);
            connection.getClient().setCallback(mMqttCallbackExtended);
            connection.getClient().setTraceCallback(new MqttTraceCallback());
            MqttConnectOptions connOpts = optionsFromModel(connectionModel);
            connection.addConnectionOptions(connOpts);
            simListener(context);
            connection.getClient().connect(connOpts, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    MQLog.d("onSuccess: Success to connect to " + connection.getClient().getServerURI());
                    connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTED);
                    connection.addAction("Client Connected");
                    if (mMQTTRegisterCallback != null) {
                        mMQTTRegisterCallback.onSuccess(connection.getId());
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    connection.changeConnectionStatus(Connection.ConnectionStatus.ERROR);
                    connection.addAction("Client failed to connect");
                    MQLog.d("onFailure: Failed to connect to " + connection.getHostName());
                    if (exception == null) {
                        if (mMQTTRegisterCallback != null) {
                            mMQTTRegisterCallback.onFailure(new RegisterException("Client failed to connect"), "Client failed to connect");
                        }
                        return;
                    }
                    if (exception instanceof MqttSecurityException) {
                        if (mMQTTRegisterCallback != null) {
                            mMQTTRegisterCallback.onFailure(new RegisterException(exception), "用户名和密码错误 ");
                        }
                    } else {
                        if (mMQTTRegisterCallback != null) {
                            mMQTTRegisterCallback.onFailure(new RegisterException(exception), "connection fail ");
                        }
                    }
                }
            });
        } catch (MqttException e) {
            if (mMQTTRegisterCallback != null) {
                mMQTTRegisterCallback.onFailure(new RegisterException(e), "MqttException occurred");
            }
        }
    }

    public ArrayList setDefaultSubscription(String[] topics) {
        ArrayList<Subscription> subscriptions = new ArrayList<>();
        Subscription mUpdateMessage = new Subscription(MQTTConstants.IOT_UPDATE + connection.getId(), 1, connection.handle(), false);
        Subscription mtMessage = new Subscription(MQTTConstants.IOT_MESSAGE + connection.getId(), 1, connection.handle(), false);
        Subscription mCMDMessage = new Subscription(MQTTConstants.IOT_CMD + connection.getId(), 1, connection.handle(), false);
        subscriptions.add(mUpdateMessage);
        subscriptions.add(mtMessage);
        subscriptions.add(mCMDMessage);
        if (topics != null) {
            for (String topic : topics) {
                subscriptions.add(new Subscription(topic, 1, connection.handle(), false));
            }
        }
        return subscriptions;

    }

    public boolean isConnected() {
        if (connection != null) {
//            MqttAndroidClient client = connection.getClient();
//            client.isConnected();
            return connection.isConnected();
        }
        return false;
    }

    private MqttConnectOptions optionsFromModel(ConnectionModel model) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(model.isCleanSession());
        connOpts.setConnectionTimeout(model.getTimeout());
        connOpts.setKeepAliveInterval(model.getKeepAlive());
        connOpts.setAutomaticReconnect(true);
        connOpts.setMaxInflight(1000);
        connOpts.setMaxReconnectDelay(60 * 1000);
        connOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        if (!TextUtils.isEmpty(model.getUsername())) {
            connOpts.setUserName(model.getUsername());
        }
        if (!TextUtils.isEmpty(model.getPassword())) {
            connOpts.setPassword(model.getPassword().toCharArray());
        }
        if (!TextUtils.isEmpty(model.getLwtTopic()) && TextUtils.isEmpty(model.getLwtMessage())) {
            connOpts.setWill(model.getLwtTopic(), model.getLwtMessage().getBytes(), model.getLwtQos(), model.isLwtRetain());
        }
        return connOpts;
    }


    /**
     * 连接成功后的处理
     */
    private void connectedHandler() {
        subscribeAllTopics();
        pushDeviceInfo();
    }

    private void pushDeviceInfo() {
        if (context != null) {
            try {
                HashMap<String, String> map = new HashMap<>();
                map.put("id", connection.getId());
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
                publishMessage(TopicType.CLIENTINFO, message, null);
            } catch (Exception e) {
                MQLog.e(e.getMessage());
            }
        }
    }

    public void updateKeepAlive(int keepAliveTime) {
        if (connection != null) {
            //TODO 设置心跳包
            ConnectionModel connectionModel = new ConnectionModel(connection);
            connectionModel.setKeepAlive(keepAliveTime);
            connection(null, connectionModel, true);
        }
    }

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

    /**
     * 订阅
     */
    private void subscribeAllTopics() {
        if (connection != null) {
            if (connection.getClient() == null || !connection.getClient().isConnected()) {
                return;
            }
            try {
                ArrayList<Subscription> subscriptions = connection.getSubscriptions();
                for (Subscription sub : subscriptions) {
                    MQLog.i("Auto-subscribing to: " + sub.getTopic() + "@ QoS: " + sub.getQos());
                    if (connection.getClient() != null && connection.getClient().isConnected()) {
                        connection.getClient().subscribe(sub.getTopic(), sub.getQos());
                    } else {
                        break;
                    }
                }
            } catch (MqttException ex) {
                MQLog.e("Failed to Auto-Subscribe: " + ex.getMessage());
            }
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage(@NonNull TopicType topicType, @NonNull String msg, @NonNull final PushCallback callback) {
        if (topicType == null) {
            if (callback != null)
                callback.onFailure(new PushException("TopicType  is must"), "TopicType  is must");
            return;
        }
        if (msg == null) {
            if (callback != null)
                callback.onFailure(new PushException("msg is must"), "msg is must");
            return;
        }
        if (connection == null) {
            if (callback != null)
                callback.onFailure(new PushException("mqtt  uninitialized"), "mqtt  uninitialized");
            return;
        }
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            String topic = null;

            if (topicType == TopicType.MQTTVOICEMESSAGERESP) {
                topic = MQTTConstants.PUBLISH_PLAYFEEDBACK + connection.getId();
            } else if (topicType == TopicType.CLIENTINFO) {
                topic = MQTTConstants.PUBLISH_UPLOADINFO + connection.getId();
            }
            MqttAndroidClient client = connection.getClient();
            if (client == null || !client.isConnected()) {
                callback.onFailure(new PushException("mqtt disconnetion"), "mqtt disconnetion");
                return;
            }
            client.publish(topic, message, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (callback != null) {
                        callback.onSuccess(asyncActionToken.getMessageId());
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (callback != null) {
                        if (exception == null) {
                            callback.onFailure(new PushException("发送消息失败"), "发送消息失败");
                        } else {
                            callback.onFailure(new PushException(exception), exception.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            MQLog.d("publishMessage: Error Publishing: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(new PushException(e), e.getMessage());
            }
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage(@NonNull String topic, @NonNull String msg, @NonNull final PushCallback callback) {
        if (TextUtils.isEmpty(topic)) {
            if (callback != null)
                callback.onFailure(new PushException("TopicType  is must"), "TopicType  is must");
            return;
        }
        if (msg == null) {
            if (callback != null)
                callback.onFailure(new PushException("msg is must"), "msg is must");
            return;
        }
        if (connection == null) {
            if (callback != null)
                callback.onFailure(new PushException("mqtt  uninitialized"), "mqtt  uninitialized");
            return;
        }

        try {
            MqttTopic.validate(topic, true/* allow wildcards */);
        } catch (Exception e) {
            if (callback != null) {
                callback.onFailure(new PushException(e), "The topic is Invalid");
            }
            return;
        }

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            MqttAndroidClient client = connection.getClient();
            if (client == null || !client.isConnected()) {
                callback.onFailure(new PushException("mqtt disconnetion"), "mqtt disconnetion");
                return;
            }
            client.publish(topic, message, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (callback != null) {
                        callback.onSuccess(asyncActionToken.getMessageId());
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (callback != null) {
                        if (exception == null) {
                            callback.onFailure(new PushException("发送消息失败"), "发送消息失败");
                        } else {
                            callback.onFailure(new PushException(exception), exception.getMessage());
                        }
                    }
                }
            });
        } catch (Exception e) {
            MQLog.d("publishMessage: Error Publishing: " + e.getMessage());
            if (callback != null) {
                callback.onFailure(new PushException(e), e.getMessage());
            }
        }
    }

    public void unregisterMQTT() {
        try {
            if (connection != null && connection.getClient()!=null) {
                connection.getClient().disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void setDebug(boolean isDebug) {
        MQLog.setDebug(isDebug);
    }
}
