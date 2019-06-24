# mqtt sdk的使用方式

### 第一步，build.gradle配置文件修改
``` java
//project build.gradle
allprojects {
    repositories {
        //添加jitpack仓库
        maven { url 'https://jitpack.io' }
    }
}

//app build.gradle
dependencies {  
    implementation 'com.github.momxmo:CmMqtt:v1.0.8'
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.1'
    implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}  
```
### 第二步，AndroidManifest.xml清单文件修改
###### 添加权限
```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
```
###### 添加接收消息广播，自定义MyMQTTReceiver继承MQTTReceiver 继承MQTTReceiver (别忘了)
``` xml
<receiver
    android:name="你的包名.MyMQTTReceiver"
    android:exported="false">
    <intent-filter>
        <!-- 接收消息透传 -->
        <action android:name="com.mqtt.push.action.PUSH_MESSAGE" />
    </intent-filter>
</receiver>

```
### 第三步，SDK初始化
###### 开启关闭debug模式
``` java
MqttManager.getInstance().setDebug(true);
```
###### 初始化（sdk默认订阅message）
``` java
以下参数有各自项目业务服务器提供：
serverUrl：mqtt服务器地址
sn:设备id(必须唯一，否则会被其他同id应用踢下线)
token:登入密码使用
topics:提供用户自定义订阅Topics使用（非必须）
MqttManager.getInstance().registerQMTT(getApplicationContext(), serverUrl, sn, token, topics, new MQTTRegisterCallback() {
    @Override
    public void onSuccess(String clientId) {
        Log.i(TAG, "注册成功：" + clientId);
    }
    @Override
    public void onFailure(Exception e, String message) {
        Log.i(TAG, "注册失败：" + message);
    }
});
```
###### 查看MQTT是否连接
``` java
 MqttManager.getInstance().isConnected();
```
###### 反初始化（断开与服务器连接，将收不到消息）
``` java
MqttManager.getInstance().unregisterMQTT();
```
### 扩展功能
###### 发布消息（或发布主题）
``` java
MqttManager.getInstance().publishMessage("你要发布的Topic", "你发布的消息", null);
```
###### [MQTT协议的详细介绍](https://mcxiaoke.gitbooks.io/mqtt-cn/content/mqtt/03-ControlPackets.html )

