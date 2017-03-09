package com.dream.player.dlna;


import java.io.IOException;

import org.cybergarage.net.HostInterface;
import org.cybergarage.upnp.device.InvalidDescriptionException;

import com.dream.player.Globals;
import com.dream.player.common.NetUtil;
import com.dream.player.common.PlayerContainer;
import com.dream.player.network.PlayerFeedback;
import com.dream.player.network.WifiDirectListener;
import com.dream.player.network.WifiDirectService;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.os.HandlerThread;

public class DlnaService extends Service{
	
	private static final String TAG = "DlnaService";
    public static final int START_DMS = 101;
    public static final int STOP_DMS = 102;
    public static final int PLAY_URI = 103;
    public static final int SHOW_IMAGE = 104;
    public static final int SETUP_WIFI_DIRECT = 105;
    public static final int SET_PLAY = 106;
    public static final int SET_VOICE_DOWN = 107;
    public static final int SET_VOICE_UP = 108;
    public static final int REGISTER_STATUS_LISTNER = 109;
    public static final int SEEK_PLAYER = 110;
    public static final int START_KTV = 111;
	private DoorbellDevice mDbDev;
	private HandlerThread mDMSThread;
	private DmsHandler mHandler;
	private Intent mWifiDirectService = null;
	private WifiDirectListener mWifiDirectListener;

	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "DlnaService onCreate");

        String ipaddress = NetUtil.getHostAddr(this);
        String friendlyName = null;
		if(ipaddress != null){
			int lastPonitPos = ipaddress.lastIndexOf(".");
			friendlyName = "Dream Player" + " " + "(" + ipaddress.substring(lastPonitPos + 1)+ ")";
		}

		HostInterface.setInterface(ipaddress);

        mDMSThread = new HandlerThread("dmsThread");
        mDMSThread.start();
        mHandler = new DmsHandler(mDMSThread.getLooper());
        
        try {
        	mDbDev = new DoorbellDevice(mHandler);
		} catch (InvalidDescriptionException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(friendlyName != null)
			mDbDev.setFriendlyName(friendlyName);
		
        mHandler.sendEmptyMessage(START_DMS);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Globals.ACTION_P2P_CONNECT);
        intentFilter.addAction(Globals.ACTION_P2P_DISCONNECT);
        intentFilter.addAction(Globals.ACTION_P2P_SETUP_TIMEOUT);
        registerReceiver(mNetStateReceiver,intentFilter);
        
    	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
    	if(playerFeedback == null){
    		playerFeedback = new PlayerFeedback();
    		PlayerContainer.getInstance().setPlayerFeedback(playerFeedback);
    	}
        
    }
    private final BroadcastReceiver mNetStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
			Log.d(TAG,"action is " + action.toString());
			if(ConnectivityManager.CONNECTIVITY_ACTION.equals(action)){
				ConnectivityManager mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);  
				NetworkInfo netInfo = mConnectivityManager.getActiveNetworkInfo();    
	            if(netInfo != null && netInfo.isAvailable()) {  
	                String name = netInfo.getTypeName();  
	                Log.d(TAG, "name is " + name);
	                if(netInfo.getType()==ConnectivityManager.TYPE_MOBILE){
	                	return;
	                }
	                String ipaddress = NetUtil.getHostAddr(DlnaService.this);
	                String beforeAddr = HostInterface.getInterface();
	            	if(ipaddress.equals(beforeAddr))
	            		return;
	                mDbDev.stop();
	                
	                String friendlyName = null;
	        		if(ipaddress != null){
	        			int lastPonitPos = ipaddress.lastIndexOf(".");
	        			friendlyName = "Dream Player" + " " + "(" + ipaddress.substring(lastPonitPos + 1)+ ")";
	        		}

	        		HostInterface.setInterface(ipaddress); 
	                try {
	                	mDbDev = new DoorbellDevice(mHandler);
	        		} catch (InvalidDescriptionException e) {
	        			e.printStackTrace();
	        		} catch (IOException e) {
	        			e.printStackTrace();
	        		}
	        		if(friendlyName != null)
	        			mDbDev.setFriendlyName(friendlyName);
	        		
	                mHandler.sendEmptyMessage(START_DMS);
	              } else {  
	             
	              }  
			} else if (Globals.ACTION_P2P_CONNECT.equals(action)){
                String ipaddress = NetUtil.getHostAddr(DlnaService.this);
                String beforeAddr = HostInterface.getInterface();
            	if(ipaddress.equals(beforeAddr))
            		return;
                mDbDev.stop();
                
                String friendlyName = null;
        		if(ipaddress != null){
        			int lastPonitPos = ipaddress.lastIndexOf(".");
        			friendlyName = "Dream Player" + " " + "(" + ipaddress.substring(lastPonitPos + 1)+ ")";
        		}

        		HostInterface.setInterface(ipaddress); 
                try {
                	mDbDev = new DoorbellDevice(mHandler);
        		} catch (InvalidDescriptionException e) {
        			e.printStackTrace();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        		if(friendlyName != null)
        			mDbDev.setFriendlyName(friendlyName);
        		
                mHandler.sendEmptyMessage(START_DMS);
				/*String ip = intent.getStringExtra(Globals.IP_EXTRA);
				Log.d(TAG, "ACTION_P2P_INFO ip " + ip);
				if(mWifiDirectListener == null){
					mWifiDirectListener = new WifiDirectListener(mHandler);
					mWifiDirectListener.init(ip);
					mWifiDirectListener.start();
					int port = mWifiDirectListener.getPort();
					PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
                	if(playerFeedback != null){
                		playerFeedback.sendMessageListener(ip, port);
                	}
				}*/
			}else if (Globals.ACTION_P2P_DISCONNECT.equals(action)){
                String ipaddress = NetUtil.getHostAddr(DlnaService.this);
                String beforeAddr = HostInterface.getInterface();
            	if(ipaddress.equals(beforeAddr))
            		return;
                mDbDev.stop();
                
                String friendlyName = null;
        		if(ipaddress != null){
        			int lastPonitPos = ipaddress.lastIndexOf(".");
        			friendlyName = "Dream Player" + " " + "(" + ipaddress.substring(lastPonitPos + 1)+ ")";
        		}

        		HostInterface.setInterface(ipaddress); 
                try {
                	mDbDev = new DoorbellDevice(mHandler);
        		} catch (InvalidDescriptionException e) {
        			e.printStackTrace();
        		} catch (IOException e) {
        			e.printStackTrace();
        		}
        		if(friendlyName != null)
        			mDbDev.setFriendlyName(friendlyName);
        		
                mHandler.sendEmptyMessage(START_DMS);
				/*if(mWifiDirectListener != null){
					mWifiDirectListener.stop();
					mWifiDirectListener = null;
				}
				if(mWifiDirectService != null){
		        	stopService(mWifiDirectService);
		        	mWifiDirectService = null;
		        }*/
			}else if (Globals.ACTION_P2P_SETUP_TIMEOUT.equals(action)){
				if(mWifiDirectListener != null){
					mWifiDirectListener.stop();
					mWifiDirectListener = null;
				}
				if(mWifiDirectService != null){
		        	stopService(mWifiDirectService);
		        	mWifiDirectService = null;
		        }
			}
		}
    };
    @Override
    public void onDestroy() {
        Log.e(TAG, "DlnaService onDestroy");
        if(mWifiDirectService != null){
        	stopService(mWifiDirectService);
        }
        if(mWifiDirectListener != null){
        	mWifiDirectListener.stop();
        	mWifiDirectListener = null;
        }
        mHandler.sendEmptyMessage(STOP_DMS);
        unregisterReceiver(mNetStateReceiver);
        super.onDestroy();
    }
    
    private void startWifiDirect(String mac){
    	if(mWifiDirectService == null){
    		mWifiDirectService = new Intent(this, WifiDirectService.class);
    		mWifiDirectService.putExtra(Globals.MAC_EXTRA, mac);
    		startService(mWifiDirectService);
    	}
    }

	private void sendStartPlayBroadcast(String url, int port,String host){
		Intent intent;
		if(url == null)
			return;
		
		if (Globals.isAudioFile(url)) {
			intent = new Intent(Globals.ACTION_PLAY_AUDIO);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			intent.putExtra(Globals.URL_EXTRA,url);
			intent.putExtra(Globals.PORT_EXTRA, port);
			intent.putExtra(Globals.IP_EXTRA,host);
			sendBroadcast(intent);
		}else if (Globals.isVideoFile(url)){
			intent = new Intent(Globals.ACTION_PLAY_VIDEO);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			intent.putExtra(Globals.URL_EXTRA,url);
			intent.putExtra(Globals.PORT_EXTRA, port);
			intent.putExtra(Globals.IP_EXTRA,host);
			sendBroadcast(intent);
		}else if (Globals.isImageFile(url)){
			intent = new Intent(Globals.ACTION_SHOW_IMAGE);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			intent.putExtra(Globals.URL_EXTRA, url);
			sendBroadcast(intent);
		}
	}
    private void sendShowImageBroadcast(int width, int height, String path){
		if (Globals.isImageFile(path)){
			Intent intent = new Intent(Globals.ACTION_SHOW_IMAGE);
			intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
					| Intent.FLAG_RECEIVER_REPLACE_PENDING);
			intent.putExtra(Globals.IMAGE_WIDTH, width);
			intent.putExtra(Globals.IMAGE_HEIGHT, height);
			intent.putExtra(Globals.PATH_EXTRA, path);
			sendBroadcast(intent);
		}
	}
	private void sendSetPlay(int state){
		Intent intent = new Intent(Globals.ACTION_SET_PLAY);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		intent.putExtra(Globals.STATE_EXTRA,state);
		sendBroadcast(intent);
	}
	private void sendSetVoiceDown(){
		Intent intent = new Intent(Globals.ACTION_SET_VOICE_DOWN);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		sendBroadcast(intent);
	}
	private void sendSetVoiceUp(){
		Intent intent = new Intent(Globals.ACTION_SET_VOICE_UP);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		sendBroadcast(intent);
	}
	private void sendSeekPlayer(int pos){
		Intent intent = new Intent(Globals.ACTION_SEEK_PLAY);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		intent.putExtra(Globals.POS_EXTRA,pos);
		sendBroadcast(intent);
	}
	private void startKtv(String host, int port){
		 
		Intent intent = new Intent(Globals.ACTION_START_KTV);
		intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY
				| Intent.FLAG_RECEIVER_REPLACE_PENDING);
		intent.putExtra(Globals.PORT_EXTRA, port);
		intent.putExtra(Globals.IP_EXTRA,host);
		sendBroadcast(intent);
	}
    class DmsHandler extends Handler{
        public DmsHandler(){
            
        }
        public DmsHandler(Looper looper){
            super(looper);
        }
        public void handleMessage(Message msg){
        	Log.d(TAG,"msg is " + msg.what);
            switch(msg.what){
                case START_DMS:
                	mDbDev.start();
                    break;
                case STOP_DMS:
                	mDbDev.stop();
                    break;
                case PLAY_URI:{
                	String url = msg.getData().getString(Globals.URL_EXTRA);
                	int port = msg.getData().getInt(Globals.PORT_EXTRA);
                	String host = msg.getData().getString(Globals.IP_EXTRA);
                	sendStartPlayBroadcast(url,port,host);
                	}
                	break;
                case SHOW_IMAGE:{
            		Bundle data = msg.getData();
            		int width = data.getInt(Globals.IMAGE_WIDTH);
            		int height = data.getInt(Globals.IMAGE_HEIGHT);
            		String path = data.getString(Globals.PATH_EXTRA);
					sendShowImageBroadcast(width,height,path);
            	}
            	break;
                case SETUP_WIFI_DIRECT:{
                	Bundle data = msg.getData();
                	String mac = data.getString(Globals.MAC_EXTRA);
                	startWifiDirect(mac);
                }
                break;
                case SET_PLAY:{
                	int state = msg.getData().getInt(Globals.STATE_EXTRA);
                	sendSetPlay(state);
                }
                break;
                case SET_VOICE_UP:{
                	sendSetVoiceUp();
                }
                break;
                case SET_VOICE_DOWN:{
                	sendSetVoiceDown();
                }
                break;
                case REGISTER_STATUS_LISTNER:{
                	String ip = msg.getData().getString(Globals.IP_EXTRA);
                	int port = msg.getData().getInt(Globals.PORT_EXTRA);
                	PlayerFeedback playerFeedback = PlayerContainer.getInstance().getPlayerFeedback();
                	if(playerFeedback == null){
                		playerFeedback = new PlayerFeedback();
                		PlayerContainer.getInstance().setPlayerFeedback(playerFeedback);
                	}
                	playerFeedback.setupSender(ip, port);
                }
                break;
                case SEEK_PLAYER:{
                	int pos = msg.getData().getInt(Globals.POS_EXTRA);
                	Log.d(TAG, "Pos " + pos);
                	sendSeekPlayer(pos);
                }
                break;
                case START_KTV:{
                	String ip = msg.getData().getString(Globals.IP_EXTRA);
                	int port = msg.getData().getInt(Globals.PORT_EXTRA);
                	startKtv(ip,port);
                }
                break;
            }
        }
    }     
}
