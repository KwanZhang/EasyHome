package com.dream.player.network;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


import com.dream.player.R;
import com.dream.player.Globals;
import com.dream.player.common.NetUtil;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectService  extends Service{
	private final static String TAG = "WifiDirectService";
    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel;
    
	private static final int DISCOVER_DELAY_MS = 10000;
	private static final int SETUP_TIMEOUT_MS = 120000;
    private static final int CMD_CREATE_GROUP = 100;
    private static final int CMD_CREATE_P2P_SERVER = 101;
    private static final int CMD_DISCOVER = 102;
    private static final int NOTIFY_CONNECT = 103;
	
    private static final int STATE_FIND = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private int mWifiDirectState;
    
    private WifiDirectHandler mWifiDirectHandler;
    private HandlerThread mWifiDirectHandlerThread;
    
    private List<WifiP2pDevice> mPeers = new ArrayList<WifiP2pDevice>();
    private String mMac = null;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	public int onStartCommand(Intent intent,int flag,int startId){
        super.onStartCommand(intent, flag, startId);
        Bundle bundle = (Bundle)intent.getExtras();
		if(bundle != null){
			mMac = bundle.getString(Globals.MAC_EXTRA);
		}
        Log.d(TAG, "mmac is " + mMac);
        return startId;
	}
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WifiDirectService onCreate");
		IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver, intentFilter);
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);
        mWifiDirectHandlerThread = new HandlerThread("WifiDirectHandleThread");
        mWifiDirectHandlerThread.start();
        mWifiDirectHandler = new WifiDirectHandler(mWifiDirectHandlerThread.getLooper());
        mWifiDirectHandler.postDelayed(mWifiDirectSetupTimeout, SETUP_TIMEOUT_MS);
    }

    public void onDestroy(){
    	mWifiDirectHandlerThread.quit();
    	if(mReceiver != null){
    		unregisterReceiver(mReceiver);
    		mReceiver = null;
    	}
    	super.onDestroy();
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
				if (mWifiP2pManager != null) {
					mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
	            }
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
				NetworkInfo networkInfo = (NetworkInfo) intent
	                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

	            if (networkInfo.isConnected()) {
	            	Log.d(TAG, "network " + networkInfo.toString());
					mWifiDirectHandler.removeCallbacks(mDiscoryLater);
	            	mWifiDirectState = STATE_CONNECTED;
	            	String addr = NetUtil.getP2pAddr();
	            	if(addr != null){
	            		mWifiDirectHandler.removeCallbacks(mWifiDirectSetupTimeout);
	            		sendP2PConnectBroadcast(addr);
	            	}
	            }else {
	            	Log.d(TAG,"sendP2PDisconnectBroadcast");
	            	String addr = NetUtil.getP2pAddr();
	            	if(addr != null){
	            		sendP2PDisconnectBroadcast();
	            	}
	            }
			} else if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)){
				Log.d(TAG, "WIFI_P2P_STATE_CHANGED_ACTION CMD_CREATE_GROUP");
				mWifiP2pManager.removeGroup(mChannel, null);
				mWifiDirectHandler.post(mDiscoryLater);
			}
		}
    };
    
    private void sendP2PConnectBroadcast(String ip){
			Intent intent = new Intent(Globals.ACTION_P2P_CONNECT);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			intent.putExtra(Globals.IP_EXTRA, ip);
			sendBroadcast(intent);
	}
    private void sendP2PDisconnectBroadcast(){
			Intent intent = new Intent(Globals.ACTION_P2P_DISCONNECT);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			sendBroadcast(intent);
	}
    private void sendP2PSetupTimeoutBroadcast(){
		Intent intent = new Intent(Globals.ACTION_P2P_SETUP_TIMEOUT);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		sendBroadcast(intent);
	}
    private Runnable mWifiDirectSetupTimeout = new Runnable(){

		@Override
		public void run() {
			Toast.makeText(WifiDirectService.this, R.string.wifi_direct_setup_timeout, Toast.LENGTH_SHORT).show();
			sendP2PSetupTimeoutBroadcast();
		}
    	
    };
    private Runnable mDiscoryLater = new Runnable(){

		@Override
		public void run() {
			Log.d(TAG, "discovery mWifiDirectState " + mWifiDirectState); 
			//if(mWifiDirectState == STATE_FIND)
			//	return;
			
			mWifiDirectState = STATE_FIND;
			mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {

                @Override
                public void onSuccess() {
                	
                	mWifiDirectHandler.removeCallbacks(mDiscoryLater);
                    mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                }

                @Override
                public void onFailure(int reasonCode) {
                    
                }
            });
		}
    	
    };
    private PeerListListener mPeerListListener = new PeerListListener(){

		@Override
		public void onPeersAvailable(WifiP2pDeviceList peers) {
			int i;
			mPeers.clear();
		    mPeers.addAll(peers.getDeviceList());
			Log.d(TAG,"mPeers size " + mPeers.size());
		    for(i = 0; i < mPeers.size(); i++){
		    	Log.d(TAG,"device address " + mPeers.get(i).deviceAddress + " mWifiDirectState " + mWifiDirectState);
		    	if((mWifiDirectState == STATE_CONNECTED) || (mWifiDirectState == STATE_CONNECTING))
		    		return;
		    	if(mPeers.get(i).deviceAddress.equals(mMac)){
		    		WifiP2pConfig config = new WifiP2pConfig();
	                config.deviceAddress = mPeers.get(i).deviceAddress;
	                config.wps.setup = WpsInfo.PBC;
	                config.groupOwnerIntent = -1;
	                try {
	                	Field sField = config.getClass().getField("netId");
	                	sField.setInt(config, -1);
					} catch (NoSuchFieldException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					}
	                Log.d(TAG, "Config is " + config.toString());
					//config.netId = WifiP2pGroup.TEMPORARY_NET_ID;
	                mWifiP2pManager.connect(mChannel, config, new ActionListener() {

	                    @Override
	                    public void onSuccess() {
	                        // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
	                    	Log.d(TAG,"connect onSuccess");
	                    	mWifiDirectState = STATE_CONNECTING;
	                    	mWifiDirectHandler.removeCallbacks(mDiscoryLater);
							mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS*2);
	                    }

	                    @Override
	                    public void onFailure(int reason) {
	                    	Log.d(TAG,"connect failed " + reason);
	                    	mWifiDirectState = STATE_FIND;
	                    	mWifiDirectHandler.post(mDiscoryLater);
	                    }
	                });
		    	}
		    }
		}
    	
    };
    class WifiDirectHandler extends Handler{
        public WifiDirectHandler(){
            
        }
        public WifiDirectHandler(Looper looper){
            super(looper);
        }

		ActionListener mCreateGroupListener = new ActionListener(){
            @Override
            public void onSuccess() {
            	Log.d(TAG,"create group success");
            }

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "create group failed CMD_CREATE_GROUP again");
				mWifiDirectHandler.sendEmptyMessageDelayed(CMD_CREATE_GROUP,5000);
            }
        };
        GroupInfoListener mGroupInfoListener = new GroupInfoListener(){

			@Override
			public void onGroupInfoAvailable(WifiP2pGroup group) {
				if(group == null){
					Log.d(TAG, "group == null");
					mWifiP2pManager.createGroup(mChannel,mCreateGroupListener);
					return;
				}
				
				if(group.isGroupOwner()){
					Log.d(TAG, "isGroupOwner ");
				}
				
				mWifiDirectHandler.removeCallbacks(mDiscoryLater);
				mWifiDirectHandler.post(mDiscoryLater);
			}
        	
        };
        public void handleMessage(Message msg){
        	Log.d(TAG,"msg is " + msg.what);
            switch(msg.what){
            case CMD_CREATE_GROUP:
            	mWifiP2pManager.requestGroupInfo(mChannel, mGroupInfoListener);
            	break;
            case CMD_DISCOVER:
            	mWifiDirectState = STATE_FIND;
            	mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                    	mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                        Log.d(TAG,"discory peers failure " + reasonCode);
                    }
                });
            	break;
			case CMD_CREATE_P2P_SERVER:{
					String ip = NetUtil.getP2pAddr();
					Log.d(TAG,"p2p Ip " + ip);
					if(ip != null){
						mWifiDirectHandler.removeCallbacks(mWifiDirectSetupTimeout);
						sendP2PConnectBroadcast(ip);
					}else{
						mWifiDirectHandler.sendEmptyMessageDelayed(CMD_CREATE_P2P_SERVER,3000);
					}
				}
				break;
            case NOTIFY_CONNECT:
            	break;
            }
        }
            
    };
}
