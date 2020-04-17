# NetConfigSDK-Android

#### Swaiot开放平台下推送SDK库

##### SDK下载地址

https://github.com/swaiot/NetConfigSDK-Android

##### 安装方式

- 1 在开放平台上获得分配到的Swaiot开发者appkey和saltkey;
- 2 依赖Swaiot开放平台提供的帐号SDK，在配网之前要先通过帐号SDK获得用户的userID和accessToken，测试阶段可使用后台分配的默认uid和ak（切勿使用于正式环境）;
- 3 在工程的libs中引入skyconfig-v1-2020-04-14-15-36.aar；
- 4 在工程的build.gradle当中增加如下:

~~~ java
    implementation 'com.isupatches:wisefy:4.0.0'
    implementation 'com.alibaba:fastjson:1.1.71.android'
~~~

> SDK用于标准的配网流程，为了能够满足各家能够自定义配网界面，所以API接口以及回调函数等都需要有一些定制化的开发，具体可以查看demo．

- 4 在工程AndroidManifest.xml代码中修改增加permissions:

~~~ java
　　<uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
~~~

- 5 参考MainAcivity的代码实现配网过程．

详细可见下载地址中的demo工程的配置及代码说明.

##### API说明

###### SkyConfigContract 类API

~~~ java
/**
         * @API  初始化配网类，回调SkyConfigCallback
         * @param uid - 用户ID,对应酷开的 open_id
         * @param token - 用户访问令牌，对应酷开的token
         * @param app_key - 应用id标示(client_id).Swaiot开放平台分配
         * @param app_secret - 应用密钥(client_secret),Swaiot开放平台分配
         * @param config_callback - 事件回调,详情查看 SkyConfigCallback
         * @return true - 初始化成功 false - 初始化失败
         */
    public static boolean do_init(String uid, String ak, String app_key, String app_secret, SkyConfigCallback config_callback);
~~~

~~~ java
	/**
     * 判断配网sdk是否初始化成功
     * @return ture - 已经初始化,false - 还未初始化
     */
    public static boolean is_init();
~~~

~~~ java
	/**
         * @API 反初始化,如果正在配置会停止
         * @return bool - 表示是否反初始化成功
         */
	public static boolean do_release()
~~~

~~~ java
	/**
                 * @API 硬件的发出的热点名称, 用于指定配网的设备, 可选, 若没有则自动配置符合条件的设备
                 *
                 * @param router_ssid - 硬件配置到家里无线路由器的热点名称
                 * @param router_password - 家里无线路由器的密码
                 * @param  device_ap - 硬件的发出的热点名称,用于指定配网的设备,可选,若没有则自动配置符合条件的设备(wifi列表的第一个)
                 * @return true-启动成功　false-启动失败
                 */
   public static boolean start_config(String router_ssid, String router_password, String device_ap)
~~~

~~~ java
	/**
     * 停止配网
     * @return true - 停止成功 false - 停止失败
     */
   public static boolean stop_config() 
~~~

~~~ java
	/**
     * 判断是否在陪网过程状态
     * @return true - 在配网过程　false - 不在配网过程
     */
    public static boolean is_config() 
~~~

~~~ java
	/**
      * @API 传入周围Wi-Fi热点列表,用于查找待配网的设备
      * @param router_ssid - 周边Wi-Fi热点列表
      */
    public static void on_ssid_list(String[] ssid_list) 
~~~

~~~ java
/**
  * @API 　传入网络变化事件
  * @param is_connect - 是否网络畅通
  * @param ssid - 当前连接的Wi-Fi名称,如果连接的不是Wi-Fi传入空字符
  */
public static void on_network_change(boolean is_connect, String ssid) 
~~~


###### SkyConfigCallback　回调API

~~~ java
  /**
         *   SkyConfigContract.startConfig 配网过程中的回调,
         *   接收到该回调,需要调用WifiUtils获取周边的ssid设备列表
         *   同样,searchForSSIDs的回调当中， 要使用SkyConfigContract.on_ssid_list(arr);回传给到库
         *   如果发现不到Swaiot直连设备的Wi-fi列表，会不停回调该方法．请注意设计timeout．
         */
	void require_ssid_list();
~~~

~~~ java
  /**
         * SkyConfigContract.startConfig 配网过程中的回调,
         * 接收到该回调,需要调用WifiUtils去连接对应的ssid和password的wifi
         * @param ssid     无线的ssid
         * @param password 对应无线ssid的密码
         */
 	void require_connect_wifi(String ssid, String password);
~~~

~~~ java
 /**
         * SkyConfigContract.startConfig 配网过程中的回调,
         * @param is_wifi_or_active 请求的网络类型，ture为当前Wi-Fi连接信息，false当前主要的网络连接信息
         *                          ture，app需要返回当前Wi-Fi的连接信息，
         *                          false，app需要当前的网络连接信息，移动网络或Wi-Fi网络都可以
         * NetworkInfoWatcher　网络监听会将wifi的状态给到库，此处给到UI处理
         */
  void require_network_info(boolean is_wifi_or_active);
~~~

~~~ java
      /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * @param progress 当前第几步
         * @param total 总共步骤数
         */
  void on_config_progress(byte progress, byte total);
~~~

~~~ java
 /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * 配置设备成功,回调设备的信息
         * @param device - 设备信息，为JSON字符
         */
  void on_config_ok(String device)

回调例子(如下是成功配置一台台灯后的返回)：
	{
	"device_id": "-",
	"product_type_id": 40,
	"product_model": "CZT-1018",
	"product_brand_id": 1,
	"module_chip": "-",
	"iot_cloud": 1,
	"protocol_version": "1.2.1",
	"mac_address": "-",
	"device_name": "台灯",
	"bind_status": 1,
	"icon": "https://fscdn.doubimeizhi.com/scloud/1/3/7/e35b44117b8d92161816dcb3f1a37"
}
~~~

~~~ java
 /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * @param code 错误的值
         * @param message 错误的消息
         */
　void on_config_fail(int code , String message);
~~~

##### Demo说明

１．由于Swaiot直连设备（WIFI模组类）配网协议需要监听操作系统的网络设置部分，所以配网NetConfigSDK依赖了

第三方库implementation 'com.isupatches:wisefy:4.0.0'．

２．配网SDK最后一部会自动绑定到Swaiot生态用户帐号下，其中ACCOUNT_TOKEN和ACCOUNT_ID是从帐号SDK当中获得的，需要配合使用．

```java
/**
 * FIXME:
 *  此处为从Swaiot帐号SDK获取到的token及uid
 */
private static String ACCOUNT_TOKEN ;
private static String ACCOINT_UID ;

/**
 * FIXME:
 *  此处为Swaiot开放平台获取到的appkey及appsalt
 */
private static String SWAIOT_AK;
private static String SWAIOT_SK;
```

3．NetworkInfoWatcher和WifiUtils是配网SDK在Android-OS上与网络相关的交互实现．非常稳定可靠，可以参考使用．其中应用层应该补充UI实现．

４．以下需要注意：

应用要保障配网过程正差，稳定，可靠，参考demo文档，在调用：

SkyConfigContract.start_config的函数接口的注意事项：

> 务必保障WIFI状态打开，可以利用如下两个函数接口进行调用：
>
> ```java
> mWifiUtils.isWirelessOpen()
> mWifiUtils.enableWifi(new EnableWifiCallbacks(){})
> ```

>　务必保障permission 可以动态申请，保障如下几个权限能被用户授权：
>
>```java
>private String[] permissions = new String[]{Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE};
>```

> 可参考demo代码中的：initPermissions() 　

另外，由于发现周边热点是一个需要一定耗时时间的接口，可能需要重复发现SSID，注意在SearchForSSIDsCallbacks回调中，

~~~ java
public void noSSIDsFound()
~~~

会调用SkyConfigContract.on_ssid_list(arr);将周边设备列表告知给到SDK，SDK发现没有符合Swaiot配网协议的直连设备之后，会重新回调require_ssid_list()请求符合配网协议的ssid列表，导致循环调用．需要在require_ssid_list()当中设定超时，配合UI提醒用户，避免长时间扫描周边wifi列表造成的性能影响．
具体实现方法可参考demo.