package com.mqtt.sdk;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mqtt.sdk.exception.PushException;
import com.mqtt.sdk.exception.RegisterException;
import com.mqtt.sdk.listener.MqttTraceCallback;
import com.mqtt.sdk.listener.PushCallback;
import com.mqtt.sdk.model.ConnectionModel;
import com.mqtt.sdk.publish.PublishTopic;
import com.mqtt.sdk.tool.MQLog;
import com.mqtt.sdk.tool.SharedPreferenceUtil;
import com.mqtt.sdk.topic.TopicBean;
import com.mqtt.sdk.topic.TopicContainer;
import com.mqtt.sdk.topic.Topics;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.net.URI;
import java.util.ArrayList;
import java.util.Map;

public class MqttManager extends BasePushManager implements PublishTopic {

    private static final String TAG = "MqttManager";
    private static MqttManager mInstance;
    private MQTTRegisterCallback mMQTTRegisterCallback;
    private Connection connection;

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

    public class MqttCallbackExtendedImpl implements MqttCallbackExtended {
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
            MQLog.d("Topic: " + topic + " isDuplicate: " + message.isDuplicate() + " messageId: " + message.getId() + "  ==> Payload: " + payload);
            if (topic != null && payload != null) {
                if (TopicContainer.getInstance().containsTopic(topic)) {
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

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            //即服务器成功delivery消息 消息发送成功ack校验
        }
    }

    ;

    public void registerQMTT(@NonNull Context mContext, String serverUrl, String sn, String token, TopicBean topicBean, @Nullable final MQTTRegisterCallback mCallback) {
        this.registerQMTT(mContext, null, serverUrl, sn, token, topicBean, mCallback);
    }

    @Override
    public void registerQMTT(@NonNull Context mContext, String productKey, String serverUrl, String sn, String token, TopicBean topicBean, @Nullable final MQTTRegisterCallback mCallback) {
        super.registerQMTT(mContext, productKey, serverUrl, sn, token, topicBean, mCallback);
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
//            NetworkModuleService.validateURI(serverUrl);
            url = new URI(serverUrl);
        } catch (Exception e) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException("URI path must be empty"), "URI path must be empty");
            }
            return;
        }
        try {
            if (topicBean != null) {
                topicBean = handlerTopic(productKey, sn, sn, topicBean);
                MqttTopic.validate(topicBean.getMessageTopic(), true);
                MqttTopic.validate(topicBean.getCmdTopic(), true);
                MqttTopic.validate(topicBean.getUploadInfoTopic(), true);
                MqttTopic.validate(topicBean.getUpdateTopic(), true);
                MqttTopic.validate(topicBean.getPlayFeedbackTopic(), true);

                TopicContainer.getInstance().setTopic(Topics.message, topicBean.getMessageTopic());
                TopicContainer.getInstance().setTopic(Topics.cmd, topicBean.getCmdTopic());
                TopicContainer.getInstance().setTopic(Topics.update, topicBean.getUpdateTopic());
                TopicContainer.getInstance().setTopic(Topics.uploadInfo, topicBean.getUploadInfoTopic());
                TopicContainer.getInstance().setTopic(Topics.playFeedback, topicBean.getPlayFeedbackTopic());
            }
        } catch (Exception e) {
            if (mCallback != null) {
                mCallback.onFailure(new RegisterException(e), "The topic is Invalid");
            }
            return;
        }
        if (connection != null) {
            ConnectionModel connectionModel = new ConnectionModel(connection);
            if (url.getHost().equals(connectionModel.getServerHostName())
                    && url.getPort() == connectionModel.getServerPort()
                    && sn.equals(connectionModel.getClientId())
                    && token.equals(connectionModel.getPassword())) {
                this.mMQTTRegisterCallback = mCallback;
                MqttAndroidClient client = connection.getClient();
                if (client != null && client.isConnected()) {
                    if (mMQTTRegisterCallback != null) {
                        mMQTTRegisterCallback.onSuccess(connection.getId());
                    }
                }
                if (topicBean != null && connection.getClient() != null) {
                    subscribeAllTopics();
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
        connectionModel.setCleanSession(false);
        connectionModel.setUsername(sn);
        connectionModel.setPassword(token);
        connectionModel.setTimeout(40);
        connectionModel.setKeepAlive(SharedPreferenceUtil.getInstance(context).getInt(SharedPreferenceUtil.KEEP_ALIVE, 30));
        connectionModel.setLwtRetain(true);
        connection(connectionModel);

    }

    private synchronized void connection(ConnectionModel connectionModel) {
        try {
            if (connection != null) {
                connection.changeConnectionStatus(Connection.ConnectionStatus.DISCONNECTING);
                if (connection.getClient() != null) {
                    try {
                        connection.getClient().disconnect();
                        connection.getClient().close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                connection.updateConnection(connectionModel.getClientId(), connectionModel.getServerHostName(), connectionModel.getServerPort(), connectionModel.isTlsConnection());
            } else {
                connection = Connection.createConnection(
                        connectionModel.getClientHandle(), connectionModel.getClientId(),
                        connectionModel.getServerHostName(), connectionModel.getServerPort(),
                        context, connectionModel.isTlsConnection());
            }
            connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);
            connection.getClient().setCallback(new MqttCallbackExtendedImpl());
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
        } catch (Exception e) {
            if (mMQTTRegisterCallback != null) {
                mMQTTRegisterCallback.onFailure(new RegisterException(e), "MqttException occurred");
            }
        }
    }

    private MqttConnectOptions optionsFromModel(ConnectionModel model) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(model.isCleanSession());
        connOpts.setConnectionTimeout(model.getTimeout());
        connOpts.setKeepAliveInterval(model.getKeepAlive());
        connOpts.setAutomaticReconnect(true);
        connOpts.setMaxInflight(1000);
//        connOpts.setMaxReconnectDelay(60 * 1000);
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


    //修改心跳包
    public void updateKeepAlive(int keepAliveTime) {
        if (connection != null) {
            ConnectionModel connectionModel = new ConnectionModel(connection);
            connectionModel.setKeepAlive(keepAliveTime);
            connection(connectionModel);
        }
    }

    /**
     * 订阅
     */
    @Override
    public void subscribeTopic(String topic) {
        if (connection != null) {
            if (connection.getClient() == null || !connection.getClient().isConnected()) {
                return;
            }
            try {
                if (connection.getClient() != null && connection.getClient().isConnected()) {
                    if (!TextUtils.isEmpty(topic)) {
                        connection.getClient().subscribe(topic, 1);
                    }
                }
            } catch (Exception ex) {
                MQLog.e("Failed to Auto-Subscribe: " + ex.getMessage());
            }
        }
    }

    /**
     * 订阅
     */
    @Override
    protected void subscribeAllTopics() {
        try {
            ArrayList<String> strings = new ArrayList<>();
            if (TopicContainer.getInstance().containsTopic(Topics.message)) {
                strings.add(TopicContainer.getInstance().getTopic(Topics.message));
            }
            if (TopicContainer.getInstance().containsTopic(Topics.update)) {
                strings.add(TopicContainer.getInstance().getTopic(Topics.update));
            }
            if (TopicContainer.getInstance().containsTopic(Topics.cmd)) {
                strings.add(TopicContainer.getInstance().getTopic(Topics.cmd));
            }
            String[] topis = new String[strings.size()];

            int[] qos = new int[strings.size()];
            for (int i = 0; i < strings.size(); i++) {
                topis[i] = strings.get(i);
                qos[i] = 1;
            }
            if (connection != null && connection.getClient() != null && strings.size() > 0) {
                connection.getClient().subscribe(topis, qos, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        MQLog.e("onSuccess to Auto-Subscribe");
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        MQLog.e("onFailure to Auto-Subscribe " + ((throwable == null) ? "" : throwable.getMessage()));
                        if (connection != null && connection.getClient().isConnected()) {
                            subscribeAllTopics();
                        }
                    }
                });
            }
        } catch (Exception ex) {
            MQLog.e("Failed to Auto-Subscribe: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * 发布消息
     */
    public void publishMessage(@NonNull Topics topics, @NonNull String msg, @NonNull final PushCallback callback) {
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
        if (connection == null) {
            if (callback != null)
                callback.onFailure(new PushException("mqtt  uninitialized"), "mqtt  uninitialized");
            return;
        }
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(msg.getBytes());
            String topic = TopicContainer.getInstance().getTopic(topics);
            MqttAndroidClient client = connection.getClient();
            if (TextUtils.isEmpty(topic)) {
                if (callback != null) {
                    callback.onFailure(new PushException("not fond Topic type"), "not fond Topic type");
                }
                return;
            }
            if (client == null || !client.isConnected()) {
                if (callback != null) {
                    callback.onFailure(new PushException("mqtt disconnetion"), "mqtt disconnetion");
                }
                return;
            }
            client.publish(topic, message, context, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (callback != null) {
                        callback.onSuccess(asyncActionToken.getMessageId() + "");
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


    @Override
    public void unregister() {
        try {
            if (connection != null && connection.getClient() != null) {
                MqttAndroidClient client = connection.getClient();
                client.disconnect();
                client.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            connection = null;
        }
    }


    public void setDebug(boolean isDebug) {
        MQLog.setDebug(isDebug);
    }

    private TopicBean handlerTopic(String productKey, String deviceName, String sn, TopicBean topicBean) {
        if (productKey == null) {
            productKey = "1007";
        }
        if (topicBean.getMessageTopic() != null) {
            String topic = topicBean.getMessageTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topic = topic.replace("{sn}", sn);
            topicBean.setMessageTopic(topic);
        }
        if (topicBean.getCmdTopic() != null) {
            String topic = topicBean.getCmdTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topic = topic.replace("{sn}", sn);
            topicBean.setCmdTopic(topic);
        }
        if (topicBean.getUpdateTopic() != null) {
            String topic = topicBean.getUpdateTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topic = topic.replace("{sn}", sn);
            topicBean.setUpdateTopic(topic);
        }
        if (topicBean.getUploadInfoTopic() != null) {
            String topic = topicBean.getUploadInfoTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topic = topic.replace("{sn}", sn);
            topicBean.setUploadInfoTopic(topic);
        }
        if (topicBean.getPlayFeedbackTopic() != null) {
            String topic = topicBean.getPlayFeedbackTopic();
            topic = topic.replace("{productKey}", productKey);
            topic = topic.replace("{deviceName}", deviceName);
            topic = topic.replace("{sn}", sn);
            topicBean.setPlayFeedbackTopic(topic);
        }
        return topicBean;
    }

    public boolean isConnected() {
        if (connection != null) {
            MqttAndroidClient client = connection.getClient();
            if (client == null) {
                return false;
            } else {
                return client.isConnected();
            }
        }
        return false;
    }
}
