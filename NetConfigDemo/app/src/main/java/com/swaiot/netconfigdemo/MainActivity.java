package com.swaiot.netconfigdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.isupatches.wisefy.callbacks.EnableWifiCallbacks;
import com.isupatches.wisefy.callbacks.SearchForSSIDsCallbacks;
import com.skyworth.config.ffi.SkyConfigCallback;
import com.skyworth.config.ffi.SkyConfigContract;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SkyConfigDemo";

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

    /**
     * 配网SDK需要用到的权限
     */
    private String[] permissions = new String[]{Manifest.permission.INTERNET,Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.CHANGE_WIFI_STATE};
    private static final int OPEN_SET_REQUEST_CODE = 100;

    /**
     * 应用需要监听网络变化,可自行实现,也可直接使用NetworkInfoWatcher,
     * 需要传入到SDK
     */
    private NetworkInfoWatcher sNetworkInfoWatcher;

    /**
     * 应用需要监听WiFi的SSID变化,此处采用了 com.isupatches:wisefy:4.0.0 第三方库,也可以自行实现
     */
    private WifiUtils mWifiUtils;

    /**
     * 启动配网后，如果周边环境没有符合Swaiot设备的Wi-Fi，会不停搜索周边的Wi-Fi名称
     * 
     */
    private static int FIND_SWAIOT_AP_TIMEOUT = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //　初始化网络信息监听类
        sNetworkInfoWatcher = new NetworkInfoWatcher();

        //　初始化wifi连接｜切换SSID类　回调MyWifiSsidSearchCallBack
        mWifiUtils = new WifiUtils(getApplicationContext(), new MyWifiSsidSearchCallBack());

        /**
         * @API  初始化配网类，回调MyNetConfigCallBack
         * @param uid - 用户ID,对应酷开的 open_id
         * @param token - 用户访问令牌，对应酷开的token
         * @param app_key - 应用id标示(client_id).Swaiot开放平台分配
         * @param app_secret - 应用密钥(client_secret),Swaiot开放平台分配
         * @param config_callback - 事件回调,详情查看 SkyConfigCallback
         */
        SkyConfigContract.do_init(ACCOINT_UID, ACCOUNT_TOKEN,SWAIOT_AK, SWAIOT_SK, new MyNetConfigCallBack());

        SkyConfigContract.is_init();

        //　获取当前wifi的信息
        final NetworkInfoWatcher.SkyNetworkInfo info = sNetworkInfoWatcher.getWifiInfo();

        /**
         * @API 　传入网络变化事件
         * @param is_connect - 是否网络畅通
         * @param ssid - 当前连接的Wi-Fi名称,如果连接的不是Wi-Fi传入空字符
         */
        SkyConfigContract.on_network_change(info.isConnect, info.ssid);

        //　监听网络变化
        sNetworkInfoWatcher.init(getApplicationContext(), new NetworkInfoWatcher.ConnectInfoCallback() {
            @Override
            public void onWifiInfo(final NetworkInfoWatcher.SkyNetworkInfo info) {
                Log.d(TAG, "onWifiInfo: " + info);
                /**
                 * @API 　传入网络变化事件
                 * @param is_connect - 是否网络畅通
                 * @param ssid - 当前连接的Wi-Fi名称,如果连接的不是Wi-Fi传入空字符
                 */
                SkyConfigContract.on_network_change(info.isConnect, info.ssid);
            }
        });



        Button btnStartCfg = findViewById(R.id.startconfigBtn);
        btnStartCfg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                configDeviceToSSID();
                initPermissions();
            }
        });
    }

    @Override
    protected void onDestroy() {
        /**
         * @API 反初始化,如果正在配置会停止
         * @return bool - 表示是否反初始化成功
         */
        SkyConfigContract.do_release();
        super.onDestroy();
    }

    //调用此方法判断是否拥有权限
    private void initPermissions() {
        if (lacksPermission()) {//判断是否拥有权限
            //请求权限，第二参数权限String数据，第三个参数是请求码便于在onRequestPermissionsResult 方法中根据code进行判断
            ActivityCompat.requestPermissions(this, permissions, OPEN_SET_REQUEST_CODE);
        } else {
            //拥有权限执行操作
//            configDeviceToSSID("","","");
            if(!mWifiUtils.isWirelessOpen()){
                mWifiUtils.enableWifi(new EnableWifiCallbacks() {
                    @Override
                    public void failureEnablingWifi() {
                        Log.d(TAG,"failureEnablingWifi");
                    }

                    @Override
                    public void wifiEnabled() {
                        Log.d(TAG,"wifiEnabled");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                EditText routeSSidTxt = findViewById(R.id.routeSsidId);
                                EditText routePwdTxt = findViewById(R.id.routePasswordId);

                                configDeviceToSSID(routeSSidTxt.getText().toString(),routePwdTxt.getText().toString(),"");
                            }
                        });
                    }

                    @Override
                    public void wisefyFailure(int i) {

                    }
                });
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        EditText routeSSidTxt = findViewById(R.id.routeSsidId);
                        EditText routePwdTxt = findViewById(R.id.routePasswordId);

                        configDeviceToSSID(routeSSidTxt.getText().toString(),routePwdTxt.getText().toString(),"");
                    }
                });
            }
        }
    }

    //如果返回true表示缺少权限
    public boolean lacksPermission() {
        for (String permission : permissions) {
            //判断是否缺少权限，true=缺少权限
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){//响应Code
            case OPEN_SET_REQUEST_CODE:
                if (grantResults.length > 0) {
                    for(int i = 0; i < grantResults.length; i++){
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"未拥有相应权限",Toast.LENGTH_LONG).show();
                            return;
                        }
                    }
                    //拥有权限执行操作
                } else {
                    Toast.makeText(this,"未拥有相应权限", Toast.LENGTH_LONG).show();
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }
    }


    /**
     *  监听ssid的变化及回调
     */
    public class MyWifiSsidSearchCallBack implements SearchForSSIDsCallbacks{

        /**
         * 搜索ssid结束的回调,没有发现Swaiot设备的ssid,
         * 过滤规则:查看WifiUtils中关于SkyLink的过滤规则表达
         */
        @Override
        public void noSSIDsFound() {
            Log.d(TAG," MyWifiSsidSearchCallBack noSSIDsFound ");
            final String[] arr= {};
            /**
             * @API 传入周围Wi-Fi热点列表,用于查找待配网的设备
             * @param router_ssid - 周边Wi-Fi热点列表
             */
            SkyConfigContract.on_ssid_list(arr);
        }

        /**
         * 搜索ssid结束的回调,发现了符合Swaiot设备过滤规则的ssid列表,
         *
         * @param list ssid名称列表
         */
        @Override
        public void retrievedSSIDs(List<String> list) {
            if(null!=list){
                final String[] arr = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i);
                    Log.d(TAG," MyWifiSsidSearchCallBack retrievedSSIDs ssid = "+ list.get(i));
                }
                /**
                 * @API 传入周围Wi-Fi热点列表,用于查找待配网的设备
                 * @param router_ssid - 周边Wi-Fi热点列表
                 */
                SkyConfigContract.on_ssid_list(arr);
            }
        }

        /**
         *
         * com.isupatches:wisefy 库中关于搜索设备的错误回调
         * @param errCode
         *
         *  DEFAULT_PRECHECK_RETURN_CODE,
         *  MISSING_PARAMETER,
         *  ETWORK_ALREADY_CONFIGURED
         */
        @Override
        public void wisefyFailure(int errCode) {
            Log.d(TAG," MyWifiSsidSearchCallBack wisefyFailure errCode = "+ errCode);
        }
    }

    public class MyWifiConnectCallBack implements WifiUtils.WifiConnectCallback{

        /**
         * 连接到Wi-Fi失败了
         * 网络监听会将wifi的状态给到库，此处给到UI处理
         * @param code　连接WIFI失败的code
         * @param msg 连接WIFI失败的消息
         */
        @Override
        public void onConnectFail(int code, String msg) {
            Log.d(TAG,"onConnectFail code = " +code +"  msg = " + msg);
        }

        /**
         * 　连接到WIFI成功了的回调
         * 　网络监听会将wifi的状态给到库，此处给到UI处理
         */
        @Override
        public void onConnectOk() {
            Log.d(TAG,"onConnectOk");
        }
    }

    public class MyNetConfigCallBack implements SkyConfigCallback{

//        1 long startTime=System.currentTimeMillis();   //获取开始时间
//2 doSomeThing();  //测试的代码段
//3 long endTime=System.currentTimeMillis(); //获取结束时间
//4 System.out.println("程序运行时间： "+(end-start)+"ms");
        long startTime = 0;
        /**
         *   SkyConfigContract.startConfig 配网过程中的回调,
         *   接收到该回调,需要调用WifiUtils获取周边的ssid设备列表
         *   同样,searchForSSIDs的回调当中， 要使用SkyConfigContract.on_ssid_list(arr);回传给到库
         *   如果发现不到Swaiot直连设备的Wi-fi列表，会不停回调该方法．请注意设计timeout．
         */
        @Override
        public void require_ssid_list() {
            Log.d(TAG," SkyConfig require_ssid_list ");
            if(null!=mWifiUtils && startTime == 0){
                startTime  = System.currentTimeMillis();
                mWifiUtils.searchForSSIDs(MainActivity.this);
            }else{
                if(null!=mWifiUtils){
                    long currTime = System.currentTimeMillis();
                    if(currTime - startTime < 20 *1000){
                        // 20ｓ　
                        mWifiUtils.searchForSSIDs(MainActivity.this);
                    }else{
                        Log.d(TAG,"end search");

                        //　扫描不到符合Swaiot协议的Wi-Fi热点，停止配网
                        SkyConfigContract.stop_config();
                        startTime = 0;
                    }
                }
            }


        }

        /**
         * SkyConfigContract.startConfig 配网过程中的回调,
         * 接收到该回调,需要调用WifiUtils去连接对应的ssid和password的wifi
         * @param ssid     无线的ssid
         * @param password 对应无线ssid的密码
         */
        @Override
        public void require_connect_wifi(String ssid, String password) {
            Log.d(TAG," SkyConfig require_connect_wifi ssid = " + ssid + " password = "+password);
            mWifiUtils.connectWiFi(ssid,password,10000,new MyWifiConnectCallBack());
        }

        /**
         * SkyConfigContract.startConfig 配网过程中的回调,
         * @param is_wifi_or_active 请求的网络类型，ture为当前Wi-Fi连接信息，false当前主要的网络连接信息
         *                          ture，app需要返回当前Wi-Fi的连接信息，
         *                          false，app需要当前的网络连接信息，移动网络或Wi-Fi网络都可以
         * NetworkInfoWatcher　网络监听会将wifi的状态给到库，此处给到UI处理
         */
        @Override
        public void require_network_info(boolean is_wifi_or_active) {
            Log.d(TAG," SkyConfig require_network_info is_wifi_or_active = " + is_wifi_or_active);
        }

        /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * @param progress 当前第几步
         * @param total 总共步骤数
         */
        @Override
        public void on_config_progress(byte progress, byte total) {
            Log.d(TAG," SkyConfig  on_config_progress progress = " + (int)progress +" total = " + (int)total);
        }

        /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * 配置设备成功,回调设备的信息
         * @param device - 设备信息，为JSON字符
         */
        @Override
        public void on_config_ok(String device) {
            Log.d(TAG,"on_config_ok device  = "+ device);
            /**
             * @API 停止配网
             * @return bool - true 初始化成功；　false 初始化失败
             */
            SkyConfigContract.stop_config();
        }

        /**
         * SkyConfigContract.startConfig 配网过程中的回调,此处给到UI处理
         * @param code 错误的值
         * @param message 错误的消息
         */
        @Override
        public void on_config_fail(int code, String message) {
            Log.d(TAG,"on_config_fail code  = "+ code +" and message = "+message);
            /**
             * @API 停止配网
             * @return bool - true 初始化成功；　false 初始化失败
             */
            SkyConfigContract.stop_config();
        }
    }
    public void configDeviceToSSID(final String routeSSID, final String routePassword, final String deviceAp){
        (new Thread(new Runnable() {
            @Override
            public void run() {
//                SkyConfigContract.start_config();
                /**
                 * @API 硬件的发出的热点名称, 用于指定配网的设备, 可选, 若没有则自动配置符合条件的设备
                 *
                 * @param router_ssid - 硬件配置到家里无线路由器的热点名称
                 * @param router_password - 家里无线路由器的密码
                 * @param  device_ap - 硬件的发出的热点名称,用于指定配网的设备,可选,若没有则自动配置符合条件的设备(wifi列表的第一个)
                 */
                SkyConfigContract.start_config(routeSSID,routePassword,deviceAp);
            }
        })).start();
    }

}
