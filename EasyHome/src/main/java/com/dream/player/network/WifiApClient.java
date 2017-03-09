package com.dream.player.network;

import java.util.List;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

public class WifiApClient {
	private final static String TAG = "WifiApClient";
	private StringBuffer mStringBuffer = new StringBuffer();
	private List<ScanResult> mListResult;
	private ScanResult mScanResult;

	private WifiManager mWifiManager;
	private WifiInfo mWifiInfo;
	private List<WifiConfiguration> mWifiConfiguration;
	private WifiLock mWifiLock;

	public WifiApClient(Context context){
		mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = mWifiManager.getConnectionInfo();
	}

	public void openWifi(){
		if(!mWifiManager.isWifiEnabled()){
			Log.d(TAG,"enable wifi ");
			mWifiManager.setWifiEnabled(true);
		}
	}
	public int getWifiState(){
		return 0; //mWifiManager.getWifiApState();
	}
	public void closeWifi(){
		if(mWifiManager.isWifiEnabled()){
			mWifiManager.setWifiEnabled(false);
		}
	}

	public void scan(){
		mWifiManager.startScan();
		mListResult = mWifiManager.getScanResults();

		if(mListResult == null){
			Log.d(TAG, "there is no wifi ap avaliable");
		}else {
			Log.d(TAG, "there are wifi ap avaliable");
		}
	}

	public String getScanResult(){
		if(mStringBuffer != null){
			mStringBuffer = new StringBuffer();
		}

		scan();

		mListResult = mWifiManager.getScanResults();
		if(mListResult != null){
			for(int i = 0; i < mListResult.size(); i++){
				mScanResult = mListResult.get(i);
				mStringBuffer = mStringBuffer.append("No.").append(i+1)
					.append(":").append(mScanResult.SSID).append("->")
					.append(mScanResult.BSSID).append("->")
					.append(mScanResult.capabilities).append("->")
					.append(mScanResult.frequency).append("->")
					.append(mScanResult.level).append("->")
					.append(mScanResult.describeContents()).append("\n\n");
			}
		}
		Log.d(TAG, "scan result " + mStringBuffer.toString());
		return mStringBuffer.toString();
	}

	public void connect(){
		mWifiInfo = mWifiManager.getConnectionInfo();
	}

	public void disconnect(){
		int netId = getNetworkId();
		if(netId > 0){
			mWifiManager.disableNetwork(netId);
			mWifiManager.disconnect();
			mWifiInfo = null;		
		}
	}

	public int getNetworkId(){
		return (mWifiInfo == null)?0:mWifiInfo.getNetworkId();
	}

	public int getIpAddress(){
		return (mWifiInfo == null)?0:mWifiInfo.getIpAddress();
	}

	public void acquireWifiLock(){
		if(mWifiLock != null){
			mWifiLock.acquire();
		}
	}

	public void releaseWifiLock(){
		if((mWifiLock != null)&&(mWifiLock.isHeld())){
			mWifiLock.release();
		}
	}

	public void createWifiLock(){
		mWifiLock = mWifiManager.createWifiLock("WifiApClient");
	}

	public List<WifiConfiguration> getWifiConfiguration(){
		return mWifiConfiguration;
	}

	public void connectConfiguration(int index){
		if(index >= mWifiConfiguration.size()){
			return ;
		}

		//mWifiManager.enableNetwork(mWifiConfiguration.get(index), true);
	}

	public boolean addNetwork(WifiConfiguration wcfg){
		int wCfgId = mWifiManager.addNetwork(wcfg);
		return mWifiManager.enableNetwork(wCfgId,true);
	}

	 public String getMySSID(String pSSID){  
          
        while(mListResult == null){
            scan(); 
            try {  
                Thread.sleep(200) ;
            } catch (InterruptedException e) {  
                e.printStackTrace();  
            }  
        }  
          
          
        for (int i = 0; i < mListResult.size(); i++) {  
            if(mListResult.get(i).SSID.contains(pSSID)){  
                Log.d(TAG,"my SSID:" + mListResult.get(i).SSID);  
                return mListResult.get(i).SSID;
            }  
        }  
		mListResult = null;
        return null ;  
    }  
	public WifiConfiguration CreateWifiInfo(String SSID, String Password,  
		int Type) {   
		String mySSID = getMySSID(SSID) ;  
		if(mySSID == null){  
			return null ;  
		}		  
		  
		WifiConfiguration config = new WifiConfiguration();  
		config.allowedAuthAlgorithms.clear();  
		config.allowedGroupCiphers.clear();  
		config.allowedKeyManagement.clear();  
		config.allowedPairwiseCiphers.clear();	
		config.allowedProtocols.clear();  
	//		config.SSID = "\"" + SSID + "\"";  
		config.SSID = "\"" + mySSID + "\"";  

		WifiConfiguration tempConfig = this.IsExsits(SSID);  
		if (tempConfig != null) {  
			mWifiManager.removeNetwork(tempConfig.networkId);  
		}  

		if (Type == 1) // WIFICIPHER_NOPASS  
		{  
			config.wepKeys[0] = "";  
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
			config.wepTxKeyIndex = 0;  
		}  
		if (Type == 2) // WIFICIPHER_WEP  
		{  
			config.hiddenSSID = true;  
			config.wepKeys[0] = "\"" + Password + "\"";  
			config.allowedAuthAlgorithms  
					.set(WifiConfiguration.AuthAlgorithm.SHARED);  
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);  
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);  
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);  
			config.allowedGroupCiphers	
					.set(WifiConfiguration.GroupCipher.WEP104);  
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);  
			config.wepTxKeyIndex = 0;  
		}  
		if (Type == 3) // WIFICIPHER_WPA  
		{  
			config.preSharedKey = "\"" + Password + "\"";  
			config.hiddenSSID = true;  
			config.allowedAuthAlgorithms  
					.set(WifiConfiguration.AuthAlgorithm.OPEN);  
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);  
			config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);  
			config.allowedPairwiseCiphers  
					.set(WifiConfiguration.PairwiseCipher.TKIP);  
			// config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);  
			config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);  
			config.allowedPairwiseCiphers  
					.set(WifiConfiguration.PairwiseCipher.CCMP);  
			config.status = WifiConfiguration.Status.ENABLED;  
		}  
		return config;	
	}  

    private WifiConfiguration IsExsits(String SSID) {  
        List<WifiConfiguration> existingConfigs = mWifiManager  
                .getConfiguredNetworks();  
        for (WifiConfiguration existingConfig : existingConfigs) {  
            Log.d(TAG,"WifiConfiguration--SSID:"+existingConfig.SSID+",preSharedKey:"+existingConfig.preSharedKey);  
            Log.d(TAG,"WifiConfiguration--preSharedKey:"+existingConfig.preSharedKey);  
            if (existingConfig.SSID.contains(SSID)) {  
                return existingConfig;  
            }  
        }  
        return null;  
    } 

}

