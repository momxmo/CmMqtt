# mqtt sdk的使用方式

### 第一步，build.gradle配置文件修改
``` java
//project build.gradle
allprojects {
    repositories {
        //添加jitpack仓库
        maven { url 'https://jitpack.io' }
         maven {
            url "http://maven.aliyun.com/nexus/content/repositories/releases/"
        }
        maven {
            url "http://maven.aliyun.com/nexus/content/repositories/snapshots"
        }
    }
}

//app build.gradle
dependencies {  
    implementation 'com.github.momxmo:CmMqtt:v1.2.2'
    implementation 'com.aliyun.alink.linksdk:iot-linkkit:1.6.6'
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
AliyunManager.getInstance().setDebug(true);
```
###### 初始化（Mqtt注册方式）
``` java
以下参数有各自项目业务服务器提供：
productKey:产品key
serverUrl：mqtt服务器地址
sn:设备id(必须唯一，否则会被其他同id应用踢下线)
token:登入密码使用
topicBean:提供用户自定义订阅
MqttManager.getInstance().registerQMTT(getApplicationContext(), productKey, serverUrl, sn, token, topicBean, new MQTTRegisterCallback() {
            @Override
            public void onSuccess(String clientId) {
                Log.i(TAG, "注册成功：" + clientId);
            }

            @Override
            public void onFailure(Exception e, String message) {
                Log.i(TAG, "注册失败：" + message);
            }

            @Override
            public void connectionLost(Throwable cause) {
                 Log.i(TAG, "客户端掉线");
            }

            @Override
            public void reconnectComplete() {
                Log.i(TAG, "重连成功");
            }
        });
```
###### 初始化（阿里云注册方式）
``` java
以下参数有各自项目业务服务器提供：
productKey:产品key
deviceName:设备id(必须唯一，否则会被其他同id应用踢下线)
deviceSecret:设备密码
topicBean:提供用户自定义订阅
 AliyunManager.getInstance().registerAliyun(this, productKey, deviceName, deviceSecret, topicBean, new CmILinkKitConnectListener() {
            @Override
            public void onError(AError aError) {
                Log.i(TAG, "阿里注册失败"+aError.getCode()+"  "+aError.getMsg()+" "+aError.getDomain());
            }

            @Override
            public void onInitDone(Object o) {
                Log.i(TAG, "阿里注册成功");
            }
        });
```

###### 查看MQTT是否连接
``` java
 MqttManager.getInstance().isConnected();
 
 //阿里云
 AliyunManager.getInstance().isConnected();
```
###### 反初始化（断开与服务器连接，将收不到消息）
``` java
MqttManager.getInstance().unregisterMQTT();
AliyunManager.getInstance().unregisterMQTT();
```
### 扩展功能
###### 发布消息（语音播报完成反馈）
``` java
MqttManager.getInstance().publishMessage(Topics.playFeedback, "你发布的消息", null);

AliyunManager.getInstance().publishMessage(Topics.playFeedback, "你发布的消息", null);
```
###### [MQTT协议的详细介绍](https://mcxiaoke.gitbooks.io/mqtt-cn/content/mqtt/03-ControlPackets.html )

