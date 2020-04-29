package com.mqtt.sdk;

/**
 * MQTT常量
 *
 * wiki文档地址：http://wiki.com/#!/message/mqtt/index.md
 */
public class MQTTConstants {

    //mqtt android客户端版本号
    public static final String MQTT_ANDROID_SDK = "1.2.5";

    //------------------------------------消息体----------------------------------
    public final static String MQTTMESSAGE = "messageResponse";
    public final static String TOPIC = "topic";
    public final static String CLIENTID = "clientId";
    public final static String SYS_CLOSE = "/sys/close/";

    //-----------------------------------广播消息Action-----------------------------
    public static final String MQTT_RECEIVER = "com.mqtt.push.action.PUSH_MESSAGE";

    //------------------------------------------------服务器消息类型----------------------------------（硬件）
    public static final String CMD_UNBIND_TPYE = "unbind";
    public static final String CMD_LOG_TPYE = "log";
    public static final String CMD_AD_TPYE = "ad";
    public static final String CMD_UPGRADE_TPYE = "upgrade";
    public static final String CMD_CLEANCACHE_TPYE = "cleancache";


    /** Bundle key for passing a connection around by it's name **/


    /** Property name for the history field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
    public static final String historyProperty = "history";

    /** Property name for the connection status field in {@link Connection} object for use with {@link java.beans.PropertyChangeEvent} **/
    public static final String ConnectionStatusProperty = "connectionStatus";


}
