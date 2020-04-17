package com.swaiot.netconfigdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;

/**
 * Created by mark on 2018/4/26.
 */

public class NetworkInfoWatcher {
    private final static String TAG = "NetworkInfoWatcher";
    private SkyNetworkInfo mActiveNetwork = new SkyNetworkInfo();
    private SkyNetworkInfo mWifiNetwork = new SkyNetworkInfo();
    private WifiManager mWifiMgr = null;
    private ConnectivityManager mConnectiveMgr;
    private SkyNetworkInfo mNetworkInfo = null;
    private SkyNetworkInfo mWifiNetworkInfo = null;
    private Context mContext = null;
    private ConnectInfoCallback mCallback = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            try {
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(TYPE_WIFI);
//                Log.i(TAG, "mReceiver onReceive getActiveNetworkInfo " + (info == null ? "null" : info.toString()));
//                Log.i(TAG, "mReceiver onReceive getNetworkInfo TYPE_WIFI" + (info == null ? "null" : info.toString()));
                if (mNetworkInfo == null) {
                    mNetworkInfo = new SkyNetworkInfo();
                }
                if (mWifiNetworkInfo == null) {
                    mWifiNetworkInfo = new SkyNetworkInfo();
                }
                HashMap<String, Object> wifiInfo = new HashMap<>();
                boolean changed = false;
                changed = setInfo(mNetworkInfo, info, NetworkInfoWatcher.this.mActiveNetwork);
                changed = setInfo(mWifiNetworkInfo, wifiNetworkInfo, NetworkInfoWatcher.this.mWifiNetwork) || changed;
//                Log.w(TAG, "onReceive changed:" + changed);
//                Log.w(TAG, "onReceive mNetworkInfo:" + mNetworkInfo.toString());
                if (changed && mCallback != null) {
                    Log.w(TAG, "onReceive callback");
                    mCallback.onWifiInfo(mNetworkInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

//    private ConnectivityManager.NetworkCallback mNetworkCallback = null;
    public interface ConnectInfoCallback {
        void onWifiInfo(SkyNetworkInfo info);
    }

    class SkyNetworkInfo {
        Boolean isConnect = false;
        String type = "";
        String ssid = "";
        String networkId = "";

        @Override
        public String toString() {
            return "SkyNetworkInfo{" +
                    "isConnect=" + isConnect +
                    ", type='" + type + '\'' +
                    ", ssid='" + ssid + '\'' +
                    ", networkId='" + networkId + '\'' +
                    '}';
        }
    }

    private boolean setInfo(SkyNetworkInfo info, NetworkInfo networkInfo, SkyNetworkInfo skyNetworkInfo) {
        Log.d(TAG, "setInfo() called with: info = [" + info + "], networkInfo = [" + networkInfo + "], skyNetworkInfo = [" + skyNetworkInfo + "]");
        Boolean isConnectTemp;
        String typeStr = "";
        String ssid = "";
        boolean changed = false;
        if (networkInfo == null) {
            isConnectTemp = false;
            Log.w(TAG, "networkInfo == null");
        } else {
            int type = networkInfo.getType();
            Log.w(TAG, "networkInfo getState:" + networkInfo.getState());

            isConnectTemp = networkInfo.getState() == NetworkInfo.State.CONNECTED ? true : false;
            Log.w(TAG, "networkInfo mType:" + type);
            switch (type) {
                case TYPE_MOBILE:
                    typeStr = "mobile";
                    break;
                case TYPE_WIFI:
                    typeStr = "wifi";
                    break;
                case TYPE_ETHERNET:
                    typeStr = "eth";
                    break;
                default:
                    typeStr = "other";
                    break;
            }
            if (type == TYPE_WIFI) {
                ssid = getSsid();
                if (!isConnectTemp && ssid != null && !ssid.equals("")) {
//                    Log.w(TAG, "setInfo SDK_INT > Q: " + Build.VERSION.SDK_INT);
                    Pattern p = Pattern.compile("^(SKYLINK|skynj-)([0-9]+M[a-z0-9]{4}|[a-z0-9]{12})$");
                    Matcher m = p.matcher(ssid);
                    boolean b = m.matches();
                    Log.w(TAG, "setInfo ssid matches b: " + b);
                    if (b) {
                        isConnectTemp = this.mWifiNetwork == skyNetworkInfo;
                    } else if (this.mWifiNetwork == skyNetworkInfo) {
                        isConnectTemp = true;
                    }
                }
            }
        }
        info.type = typeStr;
        if (!isConnectTemp.equals(skyNetworkInfo.isConnect)) {
            changed = true;
            skyNetworkInfo.isConnect = isConnectTemp;
        }

        if (!typeStr.equals(skyNetworkInfo.type)) {
            changed = true;
            skyNetworkInfo.type = typeStr;
        }
        info.isConnect = isConnectTemp;
        if (!ssid.equals(skyNetworkInfo.ssid)) {
            changed = true;
            skyNetworkInfo.ssid = ssid;
        }
        info.ssid = ssid;
        return changed;
    }

    private boolean setInfo(HashMap<String, Object> info, SkyNetworkInfo skyNetworkInfo, String ssid, Boolean isConnect, String type, String networkId) {

        boolean changed = false;
        info.put("type", type);
        if (!isConnect.equals(skyNetworkInfo.isConnect)) {
            changed = true;
            skyNetworkInfo.isConnect = isConnect;
        }

        if (type != null && !type.equals(skyNetworkInfo.type)) {
            changed = true;
            skyNetworkInfo.type = type;
        }
        info.put("isConnect", isConnect);
        if (!ssid.equals(skyNetworkInfo.ssid)) {
            changed = true;
            skyNetworkInfo.ssid = ssid;
        }
        info.put("ssid", ssid);
        if (networkId != null && !networkId.equals(skyNetworkInfo.networkId)) {
            changed = true;
            skyNetworkInfo.networkId = networkId;
            info.put("networkId", skyNetworkInfo.networkId);
        }
        return changed;
    }
    private String getSsid() {
        String ssid = "";
        if (mWifiMgr != null) {
            WifiInfo wifiInfo = mWifiMgr.getConnectionInfo();
            ssid = wifiInfo.getSSID();
            if (ssid == null) {
                ssid = "";
            }
            ssid = ssid.replaceAll("^\"|\"$", "");
            if ("<unknown ssid>".equals(ssid) || "".equals(ssid) && mConnectiveMgr != null) {
                Log.w("WifiConnect", "getConnectionInfo mSsid: " + ssid);
                NetworkInfo network_info = mConnectiveMgr.getActiveNetworkInfo();
                if (network_info != null) {
                    ssid = network_info.getExtraInfo();
                    if (ssid == null) {
                        ssid = "";
                    }
                }
            }
            Log.w(TAG, "setInfo ssid: " + ssid);
        } else {
            Log.w(TAG, "mWifiMgr != null, not init");
        }
        return ssid;
    }
    public SkyNetworkInfo getWifiInfo() {
        SkyNetworkInfo wifiNetworkInfo = new SkyNetworkInfo();
        if (mConnectiveMgr != null) {
//            NetworkInfo info = mConnectiveMgr.getActiveNetworkInfo();
            NetworkInfo info = mConnectiveMgr.getNetworkInfo(TYPE_WIFI);
            setInfo(wifiNetworkInfo, info, this.mActiveNetwork);
//            setInfo(wifiInfo, wifiNetworkInfo, this.mWifiNetwork);
//            currentNetworkInfo.put("wifiInfo", wifiInfo);
        }
        return wifiNetworkInfo;
    }

    public void init(Context context, ConnectInfoCallback callback) {
        if (mContext == null) {
            mContext = context;
            mWifiMgr = ((WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE));
            mConnectiveMgr = ((ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE));
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.STATE_CHANGE");
            filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            context.registerReceiver(this.mReceiver, filter);
            mCallback = callback;
        }

    }

    public void release() {
        if (mContext != null) {
            mContext.unregisterReceiver(this.mReceiver);
            mContext = null;
            mConnectiveMgr = null;
            mWifiMgr = null;
            mCallback = null;
        }

    }
}
