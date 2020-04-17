package com.swaiot.netconfigdemo;

import android.Manifest;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.content.PermissionChecker;

import com.isupatches.wisefy.WiseFy;
import com.isupatches.wisefy.callbacks.ConnectToNetworkCallbacks;
import com.isupatches.wisefy.callbacks.EnableWifiCallbacks;
import com.isupatches.wisefy.callbacks.SearchForAccessPointCallbacks;
import com.isupatches.wisefy.callbacks.SearchForSSIDsCallbacks;

import java.util.HashMap;
import java.util.List;


public class WifiUtils {
    private static final String TAG = "SkyConfigDemo";
    private final WifiManager mWifiManager;
    private WiseFy mWiseFy;
    private Context mContext;
    private SearchForSSIDsCallbacks mCallbacks;
    private final String SKYLINKREGEX = "^(SKYLINK|skynj-)([0-9]+M[a-z0-9]{4}|[a-z0-9]{12})$";

    public WifiUtils(Context context, SearchForSSIDsCallbacks callbacks) {
        mWiseFy = new WiseFy.Brains(context).getSmarts();
        mWifiManager = (WifiManager)((context).getSystemService(Context.WIFI_SERVICE));
        mCallbacks = callbacks;
    }

    public boolean isWirelessOpen(){
        return mWiseFy.isWifiEnabled();
    }

    public void enableWifi(EnableWifiCallbacks enAbleWifiCb){
        mWiseFy.enableWifi(enAbleWifiCb);
    }

    public boolean searchForSSIDs(Context context/*, String regex*/) {
        Log.d(TAG, "searchForSSIDs: " + SKYLINKREGEX);
        if (context == null) {
            return false;
        }
        int permission = PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permission == PermissionChecker.PERMISSION_DENIED){
            return false;
        }
        boolean scanRet =  mWifiManager.startScan();
        Log.d(TAG, "scanRet: " + scanRet);

        mWiseFy.searchForSSIDs(SKYLINKREGEX, mCallbacks);
//        List<ScanResult> list = mWiseFy.getNearbyAccessPoints(true);
//        for (ScanResult scanResult : list) {
//            Log.d(TAG, "SSID: " + scanResult.SSID);
//        }
        return true;
    }

    public void connectWiFi(final String ssid, final String password, final int time, final WifiConnectCallback callback) {
        Log.d(TAG, "_connectUseWiseFy() called with: ssid = [" + ssid + "], password = [" + password + "], time = [" + time + "], callback = [" + callback + "]");
        boolean savedNetwork = mWiseFy.isNetworkSaved(ssid);
        final HashMap<String, Object> ret = new HashMap<>();
        ret.put("ssid", ssid);
        Log.d(TAG, "savedNetwork: " + savedNetwork);
        if (!savedNetwork) {
            mWiseFy.searchForAccessPoint(ssid, time, true, new SearchForAccessPointCallbacks() {
                @Override
                public void accessPointFound(ScanResult scanResult) {
                    Log.d(TAG, "accessPointFound: " + scanResult.toString());
                    HashMap<String, Object> ret = new HashMap<>();
                    if (!addNetwork(ret, scanResult, password, callback)) {
                        return;
                    }
                    connectToSavedNetwork(ssid, password, time, callback);
                }

                @Override
                public void accessPointNotFound() {
                    Log.e(TAG, "connectWifiWithPassword searchForAccessPoint accessPointNotFound ssid = [" + ssid + "]");
                    callback.onConnectFail(-4, "accessPointNotFound");

                }

                @Override
                public void wisefyFailure(int i) {
                    Log.e(TAG, "connectWifiWithPassword searchForAccessPoint wisefyFailure ssid = [" + ssid + "]");
                    callback.onConnectFail(-5, "searchForAccessPoint wisefyFailure");
                }
            });
        }
        else {
            connectToSavedNetwork(ssid, password, time, callback);
        }
    }

    private Boolean addNetwork(HashMap<String, Object> ret, ScanResult scanResult, String password, WifiConnectCallback callback) {
        Log.d(TAG, "addNetwork() called with: ret = [" + ret + "], scanResult = [" + scanResult + "], password = [" + password + "], callback = [" + callback + "]");
        boolean addOK = false;
        String ssid = scanResult.SSID;
        ret.put("ssid", ssid);

        String type = "";
        WifiConfiguration conf = CreateWifiInfo(scanResult, ssid, password);
        int netID = mWifiManager.addNetwork(conf);
        addOK = netID != -1;

        if (!addOK) {
            callback.onConnectFail(-2,  type + " add failed");
            return false;
        }
        return addOK;
    }
    private void connectToSavedNetwork(final String ssid, final String password, final int time, final WifiConnectCallback callback) {
        Log.d(TAG, "connectToSavedNetwork() called with: ssid = [" + ssid + "], password = [" + password + "], time = [" + time + "], callback = [" + callback + "]");
        final HashMap<String, Object> ret = new HashMap<>();
        ret.put("ssid", ssid);
        mWiseFy.connectToNetwork(ssid, time, new ConnectToNetworkCallbacks() {

            @Override
            public void wisefyFailure(int i) {
                Log.d(TAG, "wisefyFailure: " + i);
                callback.onConnectFail(-5 , " connectToNetwork wisefyFailure");
            }

            @Override
            public void networkNotFoundToConnectTo() {
                Log.d(TAG, "networkNotFoundToConnectTo");
                callback.onConnectFail(-4,"connectToNetwork networkNotFoundToConnectTo");
            }

            @Override
            public void failureConnectingToNetwork() {
                Log.d(TAG, "failureConnectingToNetwork");
                callback.onConnectFail(-6, "connectToNetwork failureConnectingToNetwork");
            }

            @Override
            public void connectedToNetwork() {
                Log.d(TAG, "connectedToNetwork");
                callback.onConnectOk();
            }
        });

    }
    private WifiConfiguration IsExist(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }
    private WifiConfiguration CreateWifiInfo (ScanResult result,String ssid,String password){
        WifiConfiguration conf = new WifiConfiguration();

        //clear alloweds
        conf.allowedAuthAlgorithms.clear();
        conf.allowedGroupCiphers.clear();
        conf.allowedKeyManagement.clear();
        conf.allowedPairwiseCiphers.clear();
        conf.allowedProtocols.clear();

        // Quote ssid and password
        conf.SSID = String.format("\"%s\"", ssid);
        conf.preSharedKey = String.format("\"%s\"", password);
        Log.v("CreateWifiInfo","CreateWifiInfo====");
        WifiConfiguration tempConfig = this.IsExist(conf.SSID);
        if (tempConfig != null) {
//      mWifiManager.forgetNetwork(tempConfig.networkId);
            mWifiManager.removeNetwork(tempConfig.networkId);
            mWifiManager.saveConfiguration();
            Log.v("CreateWifiInfo","removeNetwork====");
        }

        String capabilities = result.capabilities;

        // appropriate ciper is need to set according to security type used
        if (capabilities.contains("WPA") || capabilities.contains("WPA2") || capabilities.contains("WPA/WPA2 PSK")) {

            // This is needed for WPA/WPA2
            // Reference - https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/wifi/java/android/net/wifi/WifiConfiguration.java#149
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);

            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);

            conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            conf.status = WifiConfiguration.Status.ENABLED;

        } else if (capabilities.contains("WEP")) {
            // This is needed for WEP
            // Reference - https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/wifi/java/android/net/wifi/WifiConfiguration.java#149
            conf.wepKeys[0] = "\"" + password + "\"";
            conf.wepTxKeyIndex = 0;
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else {
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        return conf;
    }

    public interface WifiConnectCallback {
        void onConnectFail(int code, String msg);
        void onConnectOk();
    }
}
