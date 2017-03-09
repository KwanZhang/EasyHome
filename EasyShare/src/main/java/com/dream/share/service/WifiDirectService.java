package com.dream.share.service;

import com.dream.share.DLNAContainer;
import com.dream.share.Globals;
import com.dream.share.R;
import com.dream.share.util.NetUtil;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class WifiDirectService  extends Service{
	private final static String TAG = "WifiDirectService";
	private static final int DISCOVER_DELAY_MS = 10000;
	private static final int SETUP_TIMEOUT_MS = 120000;
    private static final int CMD_CREATE_GROUP = 100;
    private static final int CMD_DISCOVER = 102;
    private static final int NOTIFY_CONNECT = 103;
    private static final int CMD_NOTIFY_PEER = 104;
    
    private static final int STATE_FIND = 0;
    private static final int STATE_CONNECTED = 2;
    
    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel;
    private int mWifiDirectState;
    
    private WifiDirectHandler mWifiDirectHandler;
    private HandlerThread mWifiDirectHandlerThread;
    private String mMac = null;
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
    public void onDestroy(){
    	Log.d(TAG,"onDestroy");
    	mWifiDirectHandler.removeCallbacks(mDiscoryLater);
    	mWifiDirectHandlerThread.quit();
    	if(mReceiver != null){
    		unregisterReceiver(mReceiver);
    		mReceiver = null;
    	}
    	super.onDestroy();
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Globals.P2PState = Globals.P2P_STATE_CONNECTING;
        Log.d(TAG, "WifiDirectService onCreate");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(mReceiver, intentFilter);
        mWifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(this, getMainLooper(), null);
        mWifiP2pManager.removeGroup(mChannel, null);
        mWifiDirectHandlerThread = new HandlerThread("WifiDirectHandleThread");
        mWifiDirectHandlerThread.start();
        
        mWifiDirectHandler = new WifiDirectHandler(mWifiDirectHandlerThread.getLooper());
        String selfMac = Globals.getSelfP2pMac(this);
        if(!selfMac.equals(Globals.NULL)){
        	mMac = selfMac;
    		ShareService client = DLNAContainer.getInstance().getDLNAClient();
    		if(client != null){
    			client.startWifiDirect(mMac);
    		}        	
        }

        mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
        mWifiDirectHandler.postDelayed(mWifiDirectSetupTimeout, SETUP_TIMEOUT_MS);
    }
    
    private void sendP2PConnectBroadcast(String ip){
		Globals.P2PState = Globals.P2P_STATE_CONNECTED;
		Intent intent = new Intent(Globals.ACTION_P2P_CONNECT);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		intent.putExtra(Globals.IP_EXTRA, ip);
		sendBroadcast(intent);
	}
    private void sendP2PDisconnectBroadcast(){
    	
    	Globals.P2PState = Globals.P2P_STATE_DISCONNECT;
		Intent intent = new Intent(Globals.ACTION_P2P_DISCONNECT);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		sendBroadcast(intent);
	}
    private void sendP2PSetupTimeoutBroadcast(){
    	
    	Globals.P2PState = Globals.P2P_STATE_DISCONNECT;
		Intent intent = new Intent(Globals.ACTION_P2P_SETUP_TIMEOUT);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		sendBroadcast(intent);
	}
    
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(TAG, "action " + action);
			if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)){
				if (mWifiP2pManager != null) {
					//mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
	            }
			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)){
				if(mWifiP2pManager != null){
					NetworkInfo networkInfo = (NetworkInfo) intent
		                    .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

		            if (networkInfo.isConnected()) {
		            	Log.d(TAG, "network " + networkInfo.toString());
		            	mWifiDirectHandler.sendEmptyMessage(NOTIFY_CONNECT);
		            	mWifiDirectHandler.removeCallbacks(mDiscoryLater);
		            	mWifiDirectState = STATE_CONNECTED;
		            	String ip = NetUtil.getP2pAddr();
		            	Log.d(TAG, "p2p ip " + ip);
		            	if(ip!= null){
		            		mWifiDirectHandler.removeCallbacks(mWifiDirectSetupTimeout);
		            		sendP2PConnectBroadcast(ip);
		            	}
		            }else {
		            	String ip = NetUtil.getP2pAddr();
		            	if(ip != null)
		            		sendP2PDisconnectBroadcast();
		            }
				}
			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)){
				WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
		        String selfMac = Globals.getSelfP2pMac(WifiDirectService.this);
		        if(selfMac.equals(Globals.NULL)){
		        	Log.d(TAG,"device.deviceAddress " + device.deviceAddress);
		        	Globals.setSelDevice(WifiDirectService.this, device.deviceAddress);
		    		ShareService client = DLNAContainer.getInstance().getDLNAClient();
		    		if(client != null){
		    			client.startWifiDirect(device.deviceAddress);
		    		}        	
		        }
				
			}
		}
    };
    
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
			if(mWifiDirectState != STATE_FIND)
				return;
			mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {

                @Override
                public void onSuccess() {
                	mWifiDirectHandler.removeCallbacks(mDiscoryLater);
                    mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                }

                @Override
                public void onFailure(int reasonCode) {
                	mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                	//mWifiDirectHandler.post(mDiscoryLater);
                }
            });
		}
    };
    
    
    class WifiDirectHandler extends Handler{
        public WifiDirectHandler(){
            
        }
        public WifiDirectHandler(Looper looper){
            super(looper);
        }
        
        public void handleMessage(Message msg){
        	Log.d(TAG,"msg is " + msg.what);
            switch(msg.what){
            case CMD_CREATE_GROUP:
            	mWifiP2pManager.createGroup(mChannel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                    	Log.d(TAG, "createGroup success");
                        //mWifiDirectHandler.sendEmptyMessage(CMD_INVITE_DEVICE);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                    	Log.d(TAG, "createGroup success");
                    }
                });
            	break;
            case CMD_DISCOVER:
            	mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {

                    @Override
                    public void onSuccess() {
                        mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(TAG,"discory peers failure " + reasonCode);
                        mWifiDirectHandler.postDelayed(mDiscoryLater, DISCOVER_DELAY_MS);
                    }
                });
            	break;
            case CMD_NOTIFY_PEER:{
            	DLNAContainer.getInstance().getDLNAClient().startWifiDirect(mMac);
            	}
            	break;
            }
        }
    };
}
